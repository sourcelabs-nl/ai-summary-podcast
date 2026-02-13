## Context

The LLM pipeline currently has two stages — `ArticleProcessor` (filtering + summarization) and `BriefingComposer` (script composition). Both resolve a single model from `podcast.llmModel` or the global `app.llm.cheap-model` default. The `ChatClientFactory` creates one `ChatClient` per podcast by resolving the user's first available LLM provider config.

This means:
- You can't assign different models to different pipeline stages
- You can't mix providers within a single pipeline run (e.g., OpenRouter for cheap filtering, Ollama for creative composition)
- There's no record of which model produced a given episode

## Goals / Non-Goals

**Goals:**
- Allow each pipeline stage to use a different named model (with its own provider)
- Provide sensible global defaults so most users don't need to configure anything
- Record which models were used when generating each episode
- Keep the user-facing configuration simple

**Non-Goals:**
- Per-user model registries (the registry is global in application.yaml)
- Dynamic model registration via API (models are defined by the operator)
- Per-article model overrides (model selection is per-stage, not per-article)
- Changing TTS provider configuration (out of scope)

## Decisions

### Decision 1: Model registry in application.yaml

Define named models in `application.yaml` under `app.llm.models`:

```yaml
app:
  llm:
    models:
      cheap:
        provider: openrouter
        model: anthropic/claude-haiku-4.5
      capable:
        provider: openrouter
        model: anthropic/claude-sonnet-4
      local:
        provider: ollama
        model: llama3
    defaults:
      filter: cheap
      compose: capable
```

Each model entry has a `provider` (matching a provider name in `UserProviderConfig`) and a `model` (the model ID to send to the API).

**Why**: Application.yaml is the natural place for operator-level configuration in Spring Boot. This keeps model definitions centralized and separate from per-user credentials.

**Alternative considered**: Storing model definitions in the database (per-user). Rejected because it adds CRUD complexity for little benefit — most self-hosted deployments will have a small, stable set of models. Users already choose from these via podcast overrides.

### Decision 2: AppProperties structure

Map the YAML config to Kotlin data classes:

```kotlin
data class LlmProperties(
    val models: Map<String, ModelDefinition> = emptyMap(),
    val defaults: StageDefaults = StageDefaults()
)

data class ModelDefinition(
    val provider: String,
    val model: String
)

data class StageDefaults(
    val filter: String = "cheap",
    val compose: String = "capable"
)
```

Remove the old `cheapModel` property.

### Decision 3: ChatClientFactory creates clients per model definition

Change `ChatClientFactory` from `createForPodcast(podcast)` to `createForModel(userId, modelDefinition)`. The factory:

1. Receives a `ModelDefinition` (provider + model ID)
2. Looks up the user's `UserProviderConfig` for that specific provider (not `firstOrNull()`)
3. Falls back to the global environment variable if the provider matches the default (openrouter → `OPENROUTER_API_KEY`)
4. Creates a `ChatClient` with the resolved base URL and API key

This means the pipeline can create different `ChatClient` instances for different stages if they use different providers.

**Why**: This is the minimal change needed to support per-model providers. Each pipeline stage resolves its own model definition and gets its own client.

### Decision 4: Model resolution service

Introduce a `ModelResolver` component that encapsulates the resolution chain:

```
podcast.llmModels[stageName] → app.llm.defaults[stageName] → registry lookup
```

The resolver:
1. Takes a `podcast` and a `stageName` (e.g., "filter", "compose")
2. Checks the podcast's `llmModels` map for a stage override
3. Falls back to the global stage default from `app.llm.defaults`
4. Looks up the resulting name in `app.llm.models`
5. Throws `IllegalArgumentException` if the name is not found in the registry

This isolates the resolution logic from `ArticleProcessor` and `BriefingComposer`.

### Decision 5: Per-podcast overrides as JSON map

Replace the `llm_model` TEXT column on `podcasts` with `llm_models` TEXT (JSON). The JSON value is a simple `{"stage": "modelName"}` map:

```json
{"compose": "local"}
```

Spring Data JDBC doesn't natively handle JSON maps for SQLite, so use a custom `ReadingConverter` / `WritingConverter` pair to serialize `Map<String, String>` to/from JSON TEXT using Jackson.

**Alternative considered**: Two explicit columns (`llm_filter_model`, `llm_compose_model`). Simpler for Spring Data, but adding a new stage later would require another migration. The JSON map is more flexible.

### Decision 6: Provider resolution by specific provider name

Change `UserProviderConfigService.resolveConfig` to accept a `provider` parameter:

```kotlin
fun resolveConfig(userId: String, category: ApiKeyCategory, provider: String): ProviderConfig?
```

This looks up `(userId, category, provider)` directly instead of picking `firstOrNull()`. The old method signature can be kept for backward compatibility (TTS resolution still uses it).

The global fallback still applies: if the requested provider is `"openrouter"` and no user config exists, fall back to `OPENROUTER_API_KEY` env var. For other providers without user config, fail with a clear error.

### Decision 7: Episode model tracking

Add two TEXT columns to the `episodes` table: `filter_model` and `compose_model`. These store the resolved model IDs (e.g., `"anthropic/claude-sonnet-4"`) at generation time — not the named model reference.

**Why resolved ID, not name**: If the operator remaps "capable" from Sonnet to GPT-4o later, existing episodes still accurately reflect what model was actually used.

## Risks / Trade-offs

- **JSON map in SQLite** → Less queryable than explicit columns. Mitigated by the fact that we never need to query by individual model overrides — we always load the full podcast.
- **Registry in application.yaml only** → Users can't add custom models without operator intervention. Acceptable for self-hosted; revisit if multi-tenant becomes a goal.
- **Different ChatClient per stage** → Slightly more overhead (two client constructions instead of one). Mitigated by the fact that client construction is lightweight and happens once per pipeline run.
- **Breaking API change** → `llmModel` field removed from podcast API. Mitigated by this being a pre-1.0 project with no external API consumers.

## Migration Plan

**Database (V8)**:
1. Add `llm_models` TEXT column to `podcasts` (nullable)
2. Migrate existing `llm_model` values: for each podcast where `llm_model IS NOT NULL`, set `llm_models = '{"filter":"<value>","compose":"<value>"}'` (preserving current behavior where the model was used for both stages)
3. Drop `llm_model` column (SQLite requires table recreation)
4. Add `filter_model` TEXT and `compose_model` TEXT columns to `episodes` (nullable, since existing episodes don't have this info)

**Rollback**: The migration is destructive (column drop). If rollback is needed, restore from backup. Acceptable for a pre-1.0 project.

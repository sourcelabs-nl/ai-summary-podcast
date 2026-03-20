## Context

Model configuration currently uses an alias indirection: models are defined with arbitrary names (`cheap`, `capable`, `opus`), and pipeline stages + podcast overrides reference these aliases. LLM and TTS costs live in separate config sections with different structures. This makes the configuration hard to read and maintain.

The codebase uses Spring Boot `@ConfigurationProperties` to bind YAML config to `AppProperties`. Models are consumed by `ModelResolver` (stage resolution), `ChatClientFactory` (provider credential lookup), and `CostEstimator` (cost calculation). The frontend settings page allows per-podcast model overrides.

## Goals / Non-Goals

**Goals:**
- Replace alias-keyed model map with provider-keyed nested map (`models.<provider>.<model>`)
- Unify LLM and TTS cost config under the same `models` structure with a `type` discriminator
- Use actual model names everywhere (YAML defaults, podcast overrides, API responses)
- Podcast `llmModels` stores `{provider, model}` objects instead of alias strings
- Frontend uses dropdowns populated from config instead of free-text input

**Non-Goals:**
- Changing how provider credentials are managed (`UserProviderConfig`)
- Adding model validation at startup (runtime errors on unknown models are acceptable)
- Changing TTS provider selection on podcasts (still uses `ttsProvider` enum)

## Decisions

### 1. Provider-keyed nested YAML structure

```yaml
app:
  models:
    openrouter:
      openai/gpt-5.4-nano:
        type: llm
        input-cost-per-mtok: 0.20
        output-cost-per-mtok: 1.25
    inworld:
      inworld-tts-1.5-max:
        type: tts
        cost-per-million-chars: 10.00
```

**Why over flat map with provider field:** The nesting makes the provider relationship visually clear and eliminates the redundant `provider` field on each entry. It also naturally groups models by the service they belong to.

**Why over keeping LLM/TTS separate:** Both are "model cost definitions". Unifying them reduces config duplication patterns and makes it easy to add new model types in the future.

### 2. `ModelReference` for podcast overrides and defaults

```kotlin
data class ModelReference(val provider: String, val model: String)
```

Used in both `StageDefaults` and `Podcast.llmModels`. This eliminates ambiguity when the same model name could theoretically exist under different providers.

**Why over just model name string:** Discussed during exploration. With just a model name, the resolver would need to scan all providers. The explicit `{provider, model}` makes lookup O(1) and unambiguous.

### 3. `ModelType` enum on each model entry

Each model has a `type: llm | tts` field. This allows the config API to expose available models filtered by type, so the frontend can populate LLM dropdowns with only LLM models and TTS dropdowns with only TTS models.

### 4. Union cost fields on `ModelCost`

```kotlin
data class ModelCost(
    val type: ModelType,
    val inputCostPerMtok: Double? = null,
    val outputCostPerMtok: Double? = null,
    val costPerMillionChars: Double? = null
)
```

All cost fields are optional. LLM models populate token costs, TTS models populate character costs. This avoids needing sealed classes or polymorphic deserialization for a simple config object.

### 5. Config property path change

Move from `app.llm.models` to `app.models` (top-level under `app`). This reflects that models are no longer LLM-specific. The `app.llm` section retains `defaults`, `max-cost-cents`, and `scoring` properties.

Remove `app.tts.cost-per-million-chars` entirely (absorbed into `app.models`).

### 6. Flyway migration for existing podcast data

A SQL migration reads `podcasts.llm_models` JSON, maps known aliases to `{provider, model}` objects using a hardcoded lookup table, and writes back the updated JSON.

Alias mapping (based on current `application.yaml`):
- `cheap` -> `{"provider": "openrouter", "model": "openai/gpt-5.4-nano"}`
- `capable` -> `{"provider": "openrouter", "model": "anthropic/claude-sonnet-4.6"}`
- `opus` -> `{"provider": "openrouter", "model": "anthropic/claude-opus-4.6"}`

### 7. YAML key quoting for Spring Boot relaxed binding

Model name keys containing `/`, `-`, or `.` (e.g. `openai/gpt-5.4-nano`, `inworld-tts-1.5-max`) must be quoted with `"[...]"` in YAML to prevent Spring Boot's relaxed property binding from interpreting them as nested paths or normalizing hyphens. Without quoting, `openai/gpt-5.4-nano` becomes `openai > gpt-5.4-nano` and `inworld-tts-1.5-max` loses its exact casing.

### 8. `LlmModelOverrides` wrapper for Spring Data JDBC

Spring Data JDBC cannot distinguish `Map<String, String>` from `Map<String, ModelReference>` due to generic type erasure. Both resolve to `Map` at runtime, causing converter conflicts. The solution is a dedicated `LlmModelOverrides` wrapper class with custom JDBC read/write converters, avoiding ambiguity with the existing `Map<String, String>` converters used by other podcast fields.

### 9. Unified settings save with toast notifications

The settings page uses a single Save button at the bottom for all tabs (including publishing). This saves podcast settings and publication targets in one action. All success/error feedback uses sonner toasts instead of inline messages.

## Risks / Trade-offs

- **YAML key quoting**: Model name keys require `"[key]"` quoting in YAML for Spring Boot compatibility. Slightly unusual syntax, but necessary and well-documented in Spring Boot docs.
- **Breaking API change**: `/config/defaults` response shape changes. Since the frontend is co-deployed, this is manageable, but both must be updated together.
- **Migration correctness**: The Flyway migration uses a hardcoded alias-to-model mapping. If podcasts reference aliases not in the mapping, they will be left as-is (broken). Acceptable since the alias set is small and fully known.

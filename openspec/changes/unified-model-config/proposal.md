## Why

The current model configuration uses an alias indirection layer (`cheap`, `capable`, `opus`) that obscures which actual models are in use. When configuring a podcast or reading the YAML, you have to mentally dereference aliases to understand what model runs where. Additionally, LLM and TTS model costs live in separate, inconsistently structured config sections. Unifying them under a single provider-keyed structure makes the configuration self-documenting and eliminates the indirection.

## What Changes

- **BREAKING**: Restructure `app.llm.models` from alias-keyed flat map to provider-keyed nested map: `app.models.<provider>.<model-name>` with a `type` field (llm/tts)
- **BREAKING**: Move TTS cost configuration from `app.tts.cost-per-million-chars` into the unified `app.models` structure
- **BREAKING**: Change `app.llm.defaults` to reference `{provider, model}` objects instead of alias strings
- **BREAKING**: Change `Podcast.llmModels` from `Map<String, String>` (alias values) to `Map<String, ModelReference>` where `ModelReference` has `provider` and `model` fields
- Update `ModelResolver` to do direct provider+model lookup instead of alias resolution
- Update `CostEstimator` to use the unified model config for both LLM and TTS cost lookups
- Update `ConfigController` `/config/defaults` to return `{provider, model}` objects and expose available models grouped by provider and type
- Update frontend settings page: replace text inputs with provider/model dropdowns, show defaults
- Flyway migration to convert existing `llm_models` JSON values from aliases to `{provider, model}` objects
- Remove `TtsProperties.costPerMillionChars` config section (absorbed into unified models)
- Remove `StageDefaults` simple string fields, replace with structured `ModelReference` defaults

## Capabilities

### New Capabilities

_None_

### Modified Capabilities

- `model-registry`: Restructure from alias-keyed flat map to provider-keyed nested map with typed model entries (llm/tts)
- `cost-tracking`: Unify LLM and TTS cost configuration under single `app.models` structure, remove separate `app.tts.cost-per-million-chars`
- `config-defaults-api`: Return `{provider, model}` objects for defaults and expose available models list with type information
- `frontend-podcast-settings`: Replace LLM model text inputs with provider/model dropdowns populated from config, show defaults

## Impact

- **Config**: `application.yaml` structure changes (both main and test)
- **Data model**: `AppProperties`, `ModelDefinition`, `StageDefaults`, `TtsProperties` all change
- **Database**: Flyway migration for `podcasts.llm_models` JSON values
- **API**: `/config/defaults` response shape changes (breaking for frontend)
- **Backend**: `ModelResolver`, `CostEstimator`, `ChatClientFactory`, `TtsPipeline`, `ConfigController`
- **Frontend**: Podcast settings page LLM model editor
- **Tests**: `ModelResolverTest`, `CostEstimatorTest`, and any tests referencing old alias structure

## Why

The LLM pipeline has two stages with fundamentally different workload profiles — article filtering/summarization (cheap, high volume) and briefing script composition (creative, once per episode). Currently, the single `podcast.llm_model` field forces the same model for both stages, and there's no way to use different providers per stage (e.g., OpenRouter for filtering, Ollama for composition). This change introduces a named model registry that decouples model definitions from their usage, making the pipeline more configurable without overcomplicating the user-facing API.

## What Changes

- Introduce a named model registry in `application.yaml` where each model has a name, provider, and model ID
- Add global stage-to-model defaults in `application.yaml` (e.g., filter → "cheap", compose → "capable")
- **BREAKING**: Replace `podcast.llm_model` (single TEXT field) with `podcast.llm_models` (JSON map of stage → model name overrides)
- Change provider resolution to look up credentials by the specific provider name from the model definition (instead of picking the first LLM provider config)
- Change `ChatClientFactory` to create clients per model definition (provider + model ID), not per podcast
- Add `filter_model` and `compose_model` columns to the `episodes` table to record the resolved model IDs used during generation
- Fail loudly at pipeline start if a referenced model name is not found in the registry

## Capabilities

### New Capabilities
- `model-registry`: Named model definitions in application.yaml with provider and model ID, plus global stage-to-model default mappings

### Modified Capabilities
- `llm-processing`: Model switching now resolves named models from the registry instead of using a raw model string; ChatClient created per model definition (not per podcast)
- `podcast-customization`: Replace single `llm_model` field with `llm_models` JSON map for per-stage model name overrides
- `user-api-keys`: Provider resolution changes from "pick first LLM config" to "look up by specific provider name from model definition"

## Impact

- **Database**: Migration to replace `llm_model` column on `podcasts`, add `llm_models` column; add `filter_model` and `compose_model` to `episodes`
- **Config**: `application.yaml` gains `app.llm.models.*` and `app.llm.defaults.*` sections; `app.llm.cheap-model` removed
- **API**: Podcast create/update endpoints change from `llmModel: String?` to `llmModels: Map<String, String>?`
- **Code**: `ChatClientFactory`, `ArticleProcessor`, `BriefingComposer`, `LlmPipeline`, `AppProperties` all modified
- **Breaking**: Existing `llm_model` values on podcasts need migration to the new format

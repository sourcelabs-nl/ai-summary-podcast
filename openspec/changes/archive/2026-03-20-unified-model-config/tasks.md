## 1. Config and Data Model

- [x] 1.1 Add `ModelType` enum (LLM, TTS) and `ModelCost` data class (type, inputCostPerMtok, outputCostPerMtok, costPerMillionChars) to `AppProperties.kt`
- [x] 1.2 Add `ModelReference` data class (provider, model) to `AppProperties.kt`
- [x] 1.3 Restructure `LlmProperties`: remove `models` map, update `StageDefaults` to use `ModelReference` instead of string
- [x] 1.4 Add `models: Map<String, Map<String, ModelCost>>` to `AppProperties` (top-level under `app`)
- [x] 1.5 Remove `costPerMillionChars` from `TtsProperties` (removed TtsProperties entirely)
- [x] 1.6 Update `application.yaml` (main): restructure models to provider-keyed nested map with type field, update defaults to `{provider, model}`, remove `tts.cost-per-million-chars`, add TTS models under their providers. Quote model name keys with `"[...]"` for Spring Boot relaxed binding compatibility.
- [x] 1.7 Update `application.yaml` (test): same restructuring as main

## 2. Model Resolution

- [x] 2.1 Update `Podcast.llmModels` type to `LlmModelOverrides?` (wrapper around `Map<String, ModelReference>` for Spring Data JDBC compatibility)
- [x] 2.2 Rewrite `ModelResolver` to accept `ModelReference` (from podcast override or defaults) and do direct `models[provider][model]` lookup
- [x] 2.3 Update `ModelResolver` return type: replace `ModelDefinition` with a `ResolvedModel` combining provider, model name, and `ModelCost`
- [x] 2.4 Update `ModelResolverTest` for new structure and scenarios

## 3. Cost Estimation

- [x] 3.1 Update `CostEstimator.estimateLlmCostCents` to accept `ModelCost` instead of `ModelDefinition`
- [x] 3.2 Update `CostEstimator.estimateTtsCostCents` to look up cost from unified `models` map (provider + model name) instead of flat `costPerMillionChars` map
- [x] 3.3 Update `CostEstimatorTest` for new signatures
- [x] 3.4 Update `TtsPipeline` to pass provider and model to the updated cost estimator

## 4. Chat Client and Pipeline

- [x] 4.1 Update `ChatClientFactory.createForModel` to accept `ResolvedModel` instead of `ModelDefinition`
- [x] 4.2 Update all `ModelResolver` callers: `LlmPipeline`, `ArticleScoreSummarizer`, `BriefingComposer`, `DialogueComposer`, `InterviewComposer`, `TopicDedupFilter`, `EpisodeRecapGenerator`
- [x] 4.3 Update `PodcastController` request/response DTOs to use `ModelReference` for `llmModels`
- [x] 4.4 Update `PodcastService` to pass through `ModelReference` maps

## 5. Config API

- [x] 5.1 Update `ConfigController` `/config/defaults` to return `{provider, model}` objects for llmModels defaults
- [x] 5.2 Add `availableModels` field to defaults response: provider -> list of `{name, type}` entries
- [x] 5.3 Update `PodcastDefaultsResponse` DTO

## 6. Database Migration

- [x] 6.1 Create Flyway migration to convert `podcasts.llm_models` JSON from alias strings to `{provider, model}` objects (hardcoded mapping: cheap/capable/opus to their current model definitions)

## 7. Frontend

- [x] 7.1 Update TypeScript types for `llmModels` (from `Record<string, string>` to `Record<string, {provider: string, model: string}>`) and `defaults` response (add `availableModels`)
- [x] 7.2 Create LLM model selector with provider/model dropdowns per stage, falling back to system defaults
- [x] 7.3 Replace key-value editor for `llmModels` in settings page with the new selector component
- [x] 7.4 Update settings page save logic to serialize `llmModels` as `{provider, model}` objects
- [x] 7.5 Add TTS model dropdown on same row as TTS provider, populated from `availableModels`
- [x] 7.6 Unify save: single Save button at bottom for all tabs (including publishing targets)
- [x] 7.7 Replace all inline save/error notifications with sonner toasts

## 8. Tests and Verification

- [x] 8.1 Fix any remaining broken tests from the restructuring (added LlmModelOverrides wrapper + JDBC converters)
- [x] 8.2 Build and verify application starts with the new config structure (671 tests pass, frontend builds)
- [x] 8.3 Verify config/defaults API returns all providers and models correctly

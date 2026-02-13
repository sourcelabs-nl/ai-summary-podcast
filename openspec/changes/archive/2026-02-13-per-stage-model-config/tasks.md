## 1. Configuration & Properties

- [x] 1.1 Add `ModelDefinition` and `StageDefaults` data classes to `AppProperties.kt`; replace `cheapModel` with `models: Map<String, ModelDefinition>` and `defaults: StageDefaults`
- [x] 1.2 Update `application.yaml`: replace `app.llm.cheap-model` with `app.llm.models` and `app.llm.defaults` sections
- [x] 1.3 Update test application.yaml/properties to use the new config structure

## 2. Database Migration

- [x] 2.1 Create `V8__per_stage_model_config.sql`: add `llm_models` TEXT to `podcasts`, migrate existing `llm_model` values, drop `llm_model` column (table recreation for SQLite), add `filter_model` and `compose_model` TEXT columns to `episodes`

## 3. Entity & Converter Changes

- [x] 3.1 Update `Podcast` entity: replace `llmModel: String?` with `llmModels: Map<String, String>?`
- [x] 3.2 Create Spring Data JDBC `ReadingConverter` and `WritingConverter` for `Map<String, String>` to/from JSON TEXT (using Jackson)
- [x] 3.3 Register the converters in Spring Data JDBC configuration
- [x] 3.4 Update `Episode` entity: add `filterModel: String?` and `composeModel: String?` fields

## 4. Model Resolution

- [x] 4.1 Create `ModelResolver` component: resolves stage name to `ModelDefinition` using podcast overrides → global defaults → registry lookup; throws `IllegalArgumentException` for unknown names
- [x] 4.2 Write tests for `ModelResolver`: podcast override, global default fallback, unknown name error, partial override

## 5. Provider Resolution

- [x] 5.1 Add `resolveConfig(userId, category, provider)` overload to `UserProviderConfigService` that looks up by specific provider name with provider-aware global fallback
- [x] 5.2 Add repository method `findByUserIdAndCategoryAndProvider` if not already present
- [x] 5.3 Write tests for provider-specific resolution: matching config, no config with fallback, no config without fallback

## 6. ChatClientFactory

- [x] 6.1 Change `ChatClientFactory` from `createForPodcast(podcast)` to `createForModel(userId, modelDefinition)` that resolves provider credentials from the model definition's provider name
- [x] 6.2 Update tests for `ChatClientFactory` (no existing tests to update)

## 7. Pipeline Integration

- [x] 7.1 Update `ArticleProcessor`: use `ModelResolver` to resolve filter model, use `ChatClientFactory.createForModel` to create the client, pass resolved model ID to `OpenAiChatOptions`
- [x] 7.2 Update `BriefingComposer`: use `ModelResolver` to resolve compose model, use `ChatClientFactory.createForModel` to create the client, pass resolved model ID to `OpenAiChatOptions`
- [x] 7.3 Update `LlmPipeline`: pass resolved model IDs to episode creation so `filterModel` and `composeModel` are recorded on the episode
- [x] 7.4 Update episode creation in `BriefingGenerationScheduler` (or wherever episodes are saved) to include the model fields
- [x] 7.5 Update existing pipeline tests to use the new model resolution flow

## 8. API Changes

- [x] 8.1 Update podcast create/update DTOs: replace `llmModel: String?` with `llmModels: Map<String, String>?`
- [x] 8.2 Update podcast response DTO to include `llmModels` instead of `llmModel`
- [x] 8.3 Update episode response DTO to include `filterModel` and `composeModel`
- [x] 8.4 Update API tests for the changed podcast and episode fields

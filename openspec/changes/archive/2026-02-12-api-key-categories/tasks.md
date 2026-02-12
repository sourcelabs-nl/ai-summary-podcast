## 1. Database Migration

- [x] 1.1 Create Flyway migration `V2__add_api_key_category.sql` that adds `category TEXT` column to `user_api_keys`, backfills existing rows (`openrouter` → `LLM`, `openai` → `TTS`), drops the old PK, sets `category` to `NOT NULL`, and creates new PK on `(user_id, category)`

## 2. Domain Model

- [x] 2.1 Create `ApiKeyCategory` enum (`LLM`, `TTS`) in the store package
- [x] 2.2 Update `UserApiKey` data class to include `category: ApiKeyCategory` field and remove provider from PK semantics

## 3. Repository

- [x] 3.1 Update `UserApiKeyRepository` — change queries to use `category` as the lookup key: `findByUserIdAndCategory`, `deleteByUserIdAndCategory`, update `save` to upsert on `(user_id, category)`, and update `findByUserId` to return both category and provider

## 4. Service

- [x] 4.1 Update `UserApiKeyService.setKey` to accept `category` and `provider` instead of just `provider`
- [x] 4.2 Update `UserApiKeyService.listProviders` to return category+provider pairs instead of just provider names
- [x] 4.3 Update `UserApiKeyService.deleteKey` to delete by `(userId, category)` instead of `(userId, provider)`
- [x] 4.4 Update `UserApiKeyService.resolveKey` to resolve by `(userId, category)` instead of `(userId, provider)`, and update the env var fallback mapping (`LLM` → `OPENROUTER_API_KEY`, `TTS` → `OPENAI_API_KEY`)

## 5. Controller & DTOs

- [x] 5.1 Update `SetApiKeyRequest` to include `provider` field alongside `apiKey`
- [x] 5.2 Update `ApiKeyProviderResponse` to include `category` and `provider` fields
- [x] 5.3 Update `UserApiKeyController` — change path variable from `{provider}` to `{category}`, add category validation (return 400 for invalid), update all handler methods to use the new service signatures

## 6. Pipeline Consumers

- [x] 6.1 Update `ChatClientFactory.createForPodcast` to call `resolveKey(userId, ApiKeyCategory.LLM)` instead of `resolveKey(userId, "openrouter")`
- [x] 6.2 Update `TtsService.createSpeechModel` to call `resolveKey(userId, ApiKeyCategory.TTS)` instead of `resolveKey(userId, "openai")`

## 7. Tests

- [x] 7.1 Update existing API key integration tests to use the new `/{category}` endpoints and request/response shapes
- [x] 7.2 Verify the Flyway migration runs cleanly on a fresh database and on a database with existing keys
- [x] 7.3 Test invalid category returns HTTP 400 on PUT and DELETE
- [x] 7.4 Test that key resolution by category works with user key, env var fallback, and missing key scenarios

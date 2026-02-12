## Why

API keys currently use a free-text `provider` field (e.g., `"openrouter"`, `"openai"`), but users have no way of knowing *what purpose* a provider serves. Since the system uses OpenRouter for LLM/chat and OpenAI for TTS, introducing explicit categories (`LLM` and `TTS`) makes it clear which keys power which part of the pipeline. This also future-proofs the model for when additional providers are supported within each category.

## What Changes

- **BREAKING**: Add a `category` column to `user_api_keys` (values: `LLM`, `TTS`). The composite primary key changes from `(user_id, provider)` to `(user_id, provider, category)` — or alternatively the category is derived from the provider and stored for clarity.
- Update API responses (`GET /users/{userId}/api-keys`) to include the `category` alongside the `provider`.
- Update the `PUT /users/{userId}/api-keys/{provider}` endpoint to accept a `category` field, validating it is one of the two supported values.
- Update key resolution logic to resolve by category (e.g., "give me the LLM key for this user") rather than only by provider name.
- Add a Flyway migration to add the `category` column to existing rows (backfill `"openrouter"` → `LLM`, `"openai"` → `TTS`).

## Capabilities

### New Capabilities

_(none — this change modifies an existing capability)_

### Modified Capabilities

- `user-api-keys`: API keys now carry a `category` (`LLM` or `TTS`) that classifies their purpose. The data model, API contract, list response, and key resolution logic all change to include and leverage this category.

## Impact

- **Database**: New `category` column on `user_api_keys`, migration to backfill existing rows.
- **API**: Response shape of `GET /users/{userId}/api-keys` gains a `category` field. `PUT` may gain a `category` field or derive it from the provider.
- **Code**: `UserApiKey` entity, `UserApiKeyRepository`, `UserApiKeyService` (resolution logic), `UserApiKeyController` (request/response DTOs), `ChatClientFactory`, and `TtsService` are all affected.
- **Consumers**: Any existing API client storing/reading keys will need to handle the new `category` field.
## Why

The LLM and TTS base URLs are hardcoded in `ChatClientFactory` and `TtsService`, making it impossible for users to point at alternative providers like Ollama for local development. The `user_api_keys` table already stores per-user provider configuration but lacks a `base_url` column. Additionally, the table name and internal naming no longer reflect what the table represents — it's provider configuration, not just API keys.

## What Changes

- **BREAKING**: Rename `user_api_keys` table to `user_provider_configs` via Flyway migration
- Add `base_url` column (nullable — defaults derived from provider name)
- Make `encrypted_api_key` nullable to support providers that don't require keys (e.g., Ollama)
- Rename all internal classes: `UserApiKey` → `UserProviderConfig`, `UserApiKeyRepository` → `UserProviderConfigRepository`, `UserApiKeyService` → `UserProviderConfigService`
- Change `resolveKey()` (returns `String?`) to `resolveConfig()` (returns a `ProviderConfig` with `apiKey` and `baseUrl`)
- Remove hardcoded base URLs from `ChatClientFactory` and `TtsService` — use base URL from resolved config
- Provider-based default base URLs: `openrouter` → `https://openrouter.ai/api`, `openai` → `https://api.openai.com`, `ollama` → `http://localhost:11434/v1`
- Env var fallback now returns both key and default base URL
- API endpoint path stays unchanged: `/users/{userId}/api-keys/{category}`
- `PUT` request body gains optional `baseUrl` field, `apiKey` becomes optional

## Capabilities

### New Capabilities

_None — this restructures an existing capability._

### Modified Capabilities

- `user-api-keys`: Rename table, add base URL support, make API key optional, rename internal types, change resolution to return config (key + base URL) instead of just a key

## Impact

- **Database**: V3 migration to rename table, add column, alter nullability
- **API**: `PUT` request body changes (apiKey optional, baseUrl added). `GET` response adds `baseUrl`. **BREAKING** for existing API consumers.
- **Internal**: All classes/methods referencing `UserApiKey` renamed to `UserProviderConfig`. `resolveKey` → `resolveConfig` changes return type — affects `ChatClientFactory` and `TtsService`.
- **Tests**: All existing user-api-key tests need renaming and updating
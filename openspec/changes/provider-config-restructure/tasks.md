## 1. Database Migration

- [x] 1.1 Create V3 Flyway migration: rename `user_api_keys` to `user_provider_configs`, add `base_url` column (nullable), make `encrypted_api_key` nullable, preserve existing data and PK `(user_id, category)`

## 2. Entity and Repository Rename

- [x] 2.1 Rename `UserApiKey` entity to `UserProviderConfig`, add nullable `baseUrl` field, make `encryptedApiKey` nullable
- [x] 2.2 Rename `UserApiKeyRepository` to `UserProviderConfigRepository`, update table name and all queries to use `user_provider_configs`

## 3. Service Layer

- [x] 3.1 Create `ProviderConfig` data class with `baseUrl: String` and `apiKey: String?`
- [x] 3.2 Create provider default base URL map (openrouter, openai, ollama) and resolution function
- [x] 3.3 Rename `UserApiKeyService` to `UserProviderConfigService`, update `setKey` → `setConfig`, `listKeys` → `listConfigs`, `deleteKey` → `deleteConfig`
- [x] 3.4 Replace `resolveKey(userId, category): String?` with `resolveConfig(userId, category): ProviderConfig?` — resolve base URL from stored value or provider default, include env var fallback with default base URLs

## 4. Controller

- [x] 4.1 Rename `UserApiKeyController` to `UserProviderConfigController` (keep endpoint path `/users/{userId}/api-keys/{category}`)
- [x] 4.2 Update `SetApiKeyRequest`: make `apiKey` optional, add optional `baseUrl`
- [x] 4.3 Update `ApiKeyResponse` → `ProviderConfigResponse`: add `baseUrl` field (resolved with default)
- [x] 4.4 Add validation: require `baseUrl` for unknown providers, require `apiKey` for providers that need it

## 5. Pipeline Consumers

- [x] 5.1 Update `ChatClientFactory` to use `resolveConfig()` — get `baseUrl` and `apiKey` from `ProviderConfig`, pass empty string as key when null
- [x] 5.2 Update `TtsService` to use `resolveConfig()` — same pattern

## 6. Tests

- [x] 6.1 Rename and rewrite `UserApiKeyServiceTest` → `UserProviderConfigServiceTest` → `UserProviderConfigServiceTest` with tests for: config resolution with base URL, Ollama (no API key), provider defaults, env var fallback with base URL, unknown provider validation
- [x] 6.2 Update `UserApiKeyControllerTest` → `UserProviderConfigControllerTest` with tests for: set config with/without apiKey, set config with/without baseUrl, unknown provider without baseUrl returns 400, list response includes baseUrl

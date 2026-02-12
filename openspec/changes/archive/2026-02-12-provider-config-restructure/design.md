## Context

The application stores per-user API keys in a `user_api_keys` table with columns `(user_id, provider, category, encrypted_api_key)`. The LLM and TTS services resolve a key by category and then use hardcoded base URLs (`https://openrouter.ai/api` in `ChatClientFactory`, `https://api.openai.com` in `TtsService`). This prevents users from pointing at alternative providers like a local Ollama instance.

The table, entity, repository, and service are all named around "API keys", but the table is evolving into per-user provider configuration (key + endpoint).

## Goals / Non-Goals

**Goals:**
- Make base URL configurable per user per category alongside the API key
- Support providers that require no API key (e.g., Ollama)
- Rename internal types to reflect the broader purpose (provider config, not just API keys)
- Provide sensible default base URLs per provider name
- Keep the existing REST endpoint path (`/users/{userId}/api-keys/{category}`) unchanged

**Non-Goals:**
- Adding Ollama-specific Spring AI dependencies (`spring-ai-starter-model-ollama`) — we use the OpenAI-compatible API that Ollama exposes
- Supporting TTS via Ollama (Ollama has no TTS capability)
- Per-podcast provider overrides (provider config stays at user level)
- Provider health checks or connectivity validation

## Decisions

### 1. Store `base_url` as a nullable column with provider-based defaults

The `base_url` column is nullable. When null, the system derives a default from the `provider` field:

| provider     | default base_url              |
|-------------|-------------------------------|
| `openrouter` | `https://openrouter.ai/api`   |
| `openai`     | `https://api.openai.com`      |
| `ollama`     | `http://localhost:11434/v1`   |

Unknown providers require an explicit base URL.

**Why not a required column?** Most users will use well-known providers. Forcing them to supply a URL they don't care about adds friction. Defaults keep the simple case simple.

**Why not store defaults in the DB?** Storing `null` and resolving at runtime means the defaults can evolve without a migration.

### 2. Make `encrypted_api_key` nullable

Ollama requires no authentication. Storing a dummy key is dishonest — nullable is cleaner. The encryption/decryption logic already returns `String`, so `decrypt` is simply not called when the value is null. `resolveConfig()` returns `apiKey: String?` in its result.

`ChatClientFactory` passes the API key to `OpenAiApi.builder().apiKey()`. For Ollama, we pass an empty string — the OpenAI-compatible endpoint ignores it.

### 3. Return a `ProviderConfig` data class instead of `String?`

Current: `resolveKey(userId, category): String?` — returns just the decrypted key.

New: `resolveConfig(userId, category): ProviderConfig?` where:
```
data class ProviderConfig(val baseUrl: String, val apiKey: String?)
```

This bundles the resolved base URL and optional key into a single return value. Consumers (`ChatClientFactory`, `TtsService`) destructure it to build their API clients.

The env var fallback path also returns a `ProviderConfig` with the default base URL for that category.

### 4. Rename via Flyway migration, not code-only

The table rename `user_api_keys` → `user_provider_configs` is done in a V3 Flyway migration. SQLite doesn't support `ALTER TABLE RENAME`, so we recreate the table (same pattern as V2).

### 5. Provider name is a free-form string, not an enum

Unlike `category` (which is an enum with known values LLM/TTS), `provider` remains a free-form string. Users might use `openrouter`, `openai`, `ollama`, `azure`, `anthropic`, etc. Enumerating all possible providers is not practical. The default base URL map handles known providers; unknown ones require an explicit URL.

## Risks / Trade-offs

- **Breaking API change** — `apiKey` becomes optional in the PUT request body, and `baseUrl` appears in GET responses. Existing clients sending `apiKey` still work. Clients not sending `apiKey` for non-Ollama providers will get a validation error (API key required unless provider is known to not need one).
  → Mitigation: The app has no external consumers yet, so breakage is acceptable.

- **Unknown provider without base URL** — A user could set provider `"azure"` without a `baseUrl`, and there's no default.
  → Mitigation: Validation at the API level: if provider has no known default and `baseUrl` is not provided, return 400.

- **Ollama not available at runtime** — User configures Ollama but it's not running.
  → Mitigation: Out of scope. The pipeline will fail with a connection error, which is clear enough.
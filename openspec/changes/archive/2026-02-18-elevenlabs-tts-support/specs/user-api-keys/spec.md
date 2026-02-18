## MODIFIED Requirements

### Requirement: Provider default base URLs
The system SHALL maintain a map of known provider names to default base URLs. When a user configures a provider without an explicit `base_url`, the system SHALL use the default for that provider. Known defaults SHALL be: `openrouter` → `https://openrouter.ai/api`, `openai` → `https://api.openai.com`, `ollama` → `http://localhost:11434/v1`, `elevenlabs` → `https://api.elevenlabs.io`. If the provider is not in the known list and no `base_url` is provided, the system SHALL reject the request with HTTP 400.

#### Scenario: Known provider without explicit base URL
- **WHEN** a `PUT /users/{userId}/api-keys/{category}` request is received with provider `"ollama"` and no `baseUrl`
- **THEN** the system stores the config with the default base URL `http://localhost:11434/v1`

#### Scenario: ElevenLabs provider without explicit base URL
- **WHEN** a `PUT /users/{userId}/api-keys/TTS` request is received with provider `"elevenlabs"` and no `baseUrl`
- **THEN** the system stores the config with the default base URL `https://api.elevenlabs.io`

#### Scenario: Unknown provider without base URL
- **WHEN** a `PUT /users/{userId}/api-keys/{category}` request is received with provider `"azure"` and no `baseUrl`
- **THEN** the system returns HTTP 400 with an error indicating a base URL is required for unknown providers

#### Scenario: Known provider with explicit base URL override
- **WHEN** a `PUT /users/{userId}/api-keys/{category}` request is received with provider `"ollama"` and `baseUrl` set to `http://192.168.1.100:11434/v1`
- **THEN** the system stores the config with the explicitly provided base URL

### Requirement: API key resolution with fallback
When the pipeline needs a provider configuration, the system SHALL resolve it by category and provider name. The `resolveConfig` method SHALL accept a `userId`, `category`, and `provider` parameter. It SHALL look up the user's config for that specific `(userId, category, provider)` combination. If found, it SHALL decrypt the API key (if present) and resolve the base URL (stored value or provider default). If no user config exists for the requested provider, it SHALL fall back to the corresponding environment variable only if the provider matches the default fallback provider (`"openrouter"` for `LLM` → `OPENROUTER_API_KEY`, `"openai"` for `TTS` → `OPENAI_API_KEY`). There is no environment variable fallback for `"elevenlabs"` — users must explicitly configure their ElevenLabs API key. The resolution SHALL return a `ProviderConfig` containing `baseUrl: String` and `apiKey: String?`. If neither a user config nor applicable env var fallback exists, the resolution SHALL fail with a clear error indicating which provider config is missing. The existing no-provider overload (resolving by category only, picking the first config) SHALL be retained for backward compatibility with TTS resolution.

#### Scenario: User has config for the requested provider
- **WHEN** the pipeline resolves config for provider "openrouter" and the user has a config for (LLM, "openrouter")
- **THEN** the system returns that config's base URL and decrypted API key

#### Scenario: User has config for a different provider only
- **WHEN** the pipeline resolves config for provider "ollama" but the user only has a config for (LLM, "openrouter")
- **THEN** the system does not use the openrouter config and checks the global fallback

#### Scenario: Provider matches global fallback
- **WHEN** the pipeline resolves config for provider "openrouter", the user has no config for openrouter, but `OPENROUTER_API_KEY` env var is set
- **THEN** the system returns the default openrouter base URL with the env var as API key

#### Scenario: Provider does not match global fallback
- **WHEN** the pipeline resolves config for provider "ollama" and the user has no config for ollama
- **THEN** the system fails with an error indicating no provider config is available for "ollama"

#### Scenario: ElevenLabs requires explicit config
- **WHEN** the pipeline resolves config for provider "elevenlabs" and the user has no ElevenLabs config
- **THEN** the system fails with an error indicating the user must configure an ElevenLabs API key

#### Scenario: TTS resolution unchanged
- **WHEN** the TTS pipeline resolves config using the category-only method (no provider parameter)
- **THEN** the system picks the first available TTS config for the user, unchanged from current behavior

#### Scenario: Spring AI auto-configuration excluded
- **WHEN** the application starts
- **THEN** no Spring AI auto-configured chat or audio beans are created, and no `spring.ai` properties are required

## MODIFIED Requirements

### Requirement: API key resolution with fallback
When the pipeline needs a provider configuration, the system SHALL resolve it by category and provider name. The `resolveConfig` method SHALL accept a `userId`, `category`, and `provider` parameter. It SHALL look up the user's config for that specific `(userId, category, provider)` combination. If found, it SHALL decrypt the API key (if present) and resolve the base URL (stored value or provider default). If no user config exists for the requested provider, it SHALL fall back to the corresponding environment variable only if the provider matches the default fallback provider (`"openrouter"` for `LLM` → `OPENROUTER_API_KEY`, `"openai"` for `TTS` → `OPENAI_API_KEY`). The resolution SHALL return a `ProviderConfig` containing `baseUrl: String` and `apiKey: String?`. If neither a user config nor applicable env var fallback exists, the resolution SHALL fail with a clear error indicating which provider config is missing. The existing no-provider overload (resolving by category only, picking the first config) SHALL be retained for backward compatibility with TTS resolution.

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

#### Scenario: TTS resolution unchanged
- **WHEN** the TTS pipeline resolves config using the category-only method (no provider parameter)
- **THEN** the system picks the first available TTS config for the user, unchanged from current behavior

#### Scenario: Spring AI auto-configuration excluded
- **WHEN** the application starts
- **THEN** no Spring AI auto-configured chat or audio beans are created, and no `spring.ai` properties are required

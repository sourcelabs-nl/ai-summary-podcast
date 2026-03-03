## MODIFIED Requirements

### Requirement: Provider default base URLs
The system SHALL maintain a map of known provider names to default base URLs. When a user configures a provider without an explicit `base_url`, the system SHALL use the default for that provider. Known defaults SHALL be: `openrouter` → `https://openrouter.ai/api`, `openai` → `https://api.openai.com`, `ollama` → `http://localhost:11434/v1`, `elevenlabs` → `https://api.elevenlabs.io`, `inworld` → `https://api.inworld.ai`. If the provider is not in the known list and no `base_url` is provided, the system SHALL reject the request with HTTP 400.

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

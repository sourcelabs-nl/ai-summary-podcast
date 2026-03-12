## MODIFIED Requirements

### Requirement: Store API key for a provider
The system SHALL store per-user provider configurations in a `user_provider_configs` table with columns: `user_id` (TEXT, FK to users), `provider` (TEXT, NOT NULL), `category` (TEXT, NOT NULL), `base_url` (TEXT, nullable), `encrypted_api_key` (TEXT, nullable, Base64-encoded IV+ciphertext), with a composite primary key of `(user_id, category, provider)`. Supported category values SHALL be `"LLM"`, `"TTS"`, and `"PUBLISHING"`. The `provider` field SHALL identify the external service (e.g., `"openrouter"`, `"openai"`, `"ollama"`, `"ftp"`, `"soundcloud"`). The API key SHALL be encrypted before storage and decrypted only when needed at runtime. The API key MAY be null for providers that do not require authentication (e.g., Ollama). For `PUBLISHING` category providers, the `api_key` value SHALL be a JSON-encoded string containing all credential fields for that provider. The `base_url` column SHALL be null for publishing providers. Each user MAY have multiple configurations per category, but SHALL have at most one configuration per category-provider combination.

#### Scenario: Set a provider config with API key
- **WHEN** a `PUT /users/{userId}/api-keys/{category}` request is received with a JSON body containing `provider` and `apiKey`, and `category` is a valid value (`LLM`, `TTS`, or `PUBLISHING`)
- **THEN** the system encrypts the API key with AES-256-GCM, stores it with the provider and category in the `user_provider_configs` table, and returns HTTP 200

#### Scenario: Set a provider config without API key
- **WHEN** a `PUT /users/{userId}/api-keys/{category}` request is received with `provider` set to `"ollama"` and no `apiKey`
- **THEN** the system stores the config with a null `encrypted_api_key` and returns HTTP 200

#### Scenario: Set API key with invalid category
- **WHEN** a `PUT /users/{userId}/api-keys/{category}` request is received with a `category` that is not `LLM`, `TTS`, or `PUBLISHING`
- **THEN** the system returns HTTP 400 with a validation error message indicating the valid categories

#### Scenario: Set publishing credentials for FTP
- **WHEN** a `PUT /users/{userId}/api-keys/PUBLISHING` request is received with `provider = "ftp"` and `apiKey` containing JSON-encoded FTP credentials
- **THEN** the system encrypts and stores the JSON string, and returns HTTP 200

#### Scenario: Set API key for non-existing user
- **WHEN** a `PUT /users/{userId}/api-keys/{category}` request is received for a user that does not exist
- **THEN** the system returns HTTP 404

#### Scenario: Set API key with missing provider
- **WHEN** a `PUT /users/{userId}/api-keys/{category}` request is received without `provider` in the body
- **THEN** the system returns HTTP 400 with a validation error message

#### Scenario: Replace an existing provider config for a category and provider
- **WHEN** a `PUT /users/{userId}/api-keys/{category}` request is received for a category and provider combination that already has a config stored
- **THEN** the system updates the base URL and encrypted key for that specific provider, leaving other providers in the same category unchanged

#### Scenario: Add a second provider to the same category
- **WHEN** a user already has an LLM config for provider `"openrouter"` and a `PUT /users/{userId}/api-keys/LLM` request is received with provider `"ollama"`
- **THEN** the system stores the new config alongside the existing one, and both configs are returned by the list endpoint

### Requirement: Provider default base URLs
The system SHALL maintain a map of known provider names to default base URLs. When a user configures a provider without an explicit `base_url`, the system SHALL use the default for that provider. Known defaults SHALL be: `openrouter` → `https://openrouter.ai/api`, `openai` → `https://api.openai.com`, `ollama` → `http://localhost:11434/v1`, `elevenlabs` → `https://api.elevenlabs.io`, `inworld` → `https://api.inworld.ai`. If the provider is not in the known list and no `base_url` is provided, the system SHALL store the config with a null `base_url`. For `PUBLISHING` category providers, the `base_url` requirement SHALL be skipped (always null).

#### Scenario: Known provider without explicit base URL
- **WHEN** a `PUT /users/{userId}/api-keys/{category}` request is received with provider `"ollama"` and no `baseUrl`
- **THEN** the system stores the config with the default base URL `http://localhost:11434/v1`

#### Scenario: ElevenLabs provider without explicit base URL
- **WHEN** a `PUT /users/{userId}/api-keys/TTS` request is received with provider `"elevenlabs"` and no `baseUrl`
- **THEN** the system stores the config with the default base URL `https://api.elevenlabs.io`

#### Scenario: Unknown provider without base URL for LLM/TTS
- **WHEN** a `PUT /users/{userId}/api-keys/LLM` request is received with provider `"azure"` and no `baseUrl`
- **THEN** the system returns HTTP 400 with an error indicating a base URL is required for unknown providers

#### Scenario: Publishing provider without base URL
- **WHEN** a `PUT /users/{userId}/api-keys/PUBLISHING` request is received with provider `"ftp"` and no `baseUrl`
- **THEN** the system stores the config with a null `base_url` (no validation error)

#### Scenario: Known provider with explicit base URL override
- **WHEN** a `PUT /users/{userId}/api-keys/{category}` request is received with provider `"ollama"` and `baseUrl` set to `http://192.168.1.100:11434/v1`
- **THEN** the system stores the config with the explicitly provided base URL

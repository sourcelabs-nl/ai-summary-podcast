## ADDED Requirements

### Requirement: Provider default base URLs
The system SHALL maintain a map of known provider names to default base URLs. When a user configures a provider without an explicit `base_url`, the system SHALL use the default for that provider. Known defaults SHALL be: `openrouter` → `https://openrouter.ai/api`, `openai` → `https://api.openai.com`, `ollama` → `http://localhost:11434/v1`. If the provider is not in the known list and no `base_url` is provided, the system SHALL reject the request with HTTP 400.

#### Scenario: Known provider without explicit base URL
- **WHEN** a `PUT /users/{userId}/api-keys/{category}` request is received with provider `"ollama"` and no `baseUrl`
- **THEN** the system stores the config with the default base URL `http://localhost:11434/v1`

#### Scenario: Unknown provider without base URL
- **WHEN** a `PUT /users/{userId}/api-keys/{category}` request is received with provider `"azure"` and no `baseUrl`
- **THEN** the system returns HTTP 400 with an error indicating a base URL is required for unknown providers

#### Scenario: Known provider with explicit base URL override
- **WHEN** a `PUT /users/{userId}/api-keys/{category}` request is received with provider `"ollama"` and `baseUrl` set to `http://192.168.1.100:11434/v1`
- **THEN** the system stores the config with the explicitly provided base URL

## MODIFIED Requirements

### Requirement: Store API key for a provider
The system SHALL store per-user provider configurations in a `user_provider_configs` table with columns: `user_id` (TEXT, FK to users), `provider` (TEXT, NOT NULL), `category` (TEXT, NOT NULL), `base_url` (TEXT, nullable), `encrypted_api_key` (TEXT, nullable, Base64-encoded IV+ciphertext), with a composite primary key of `(user_id, category)`. Supported category values SHALL be `"LLM"` and `"TTS"`. The `provider` field SHALL identify the external service (e.g., `"openrouter"`, `"openai"`, `"ollama"`). The API key SHALL be encrypted before storage and decrypted only when needed at pipeline runtime. The API key MAY be null for providers that do not require authentication (e.g., Ollama). Each user SHALL have at most one configuration per category.

#### Scenario: Set a provider config with API key
- **WHEN** a `PUT /users/{userId}/api-keys/{category}` request is received with a JSON body containing `provider` and `apiKey`, and `category` is a valid value (`LLM` or `TTS`)
- **THEN** the system encrypts the API key with AES-256-GCM, stores it with the provider and category in the `user_provider_configs` table, and returns HTTP 200

#### Scenario: Set a provider config without API key
- **WHEN** a `PUT /users/{userId}/api-keys/{category}` request is received with `provider` set to `"ollama"` and no `apiKey`
- **THEN** the system stores the config with a null `encrypted_api_key` and returns HTTP 200

#### Scenario: Set API key with invalid category
- **WHEN** a `PUT /users/{userId}/api-keys/{category}` request is received with a `category` that is not `LLM` or `TTS`
- **THEN** the system returns HTTP 400 with a validation error message indicating the valid categories

#### Scenario: Set API key for non-existing user
- **WHEN** a `PUT /users/{userId}/api-keys/{category}` request is received for a user that does not exist
- **THEN** the system returns HTTP 404

#### Scenario: Set API key with missing provider
- **WHEN** a `PUT /users/{userId}/api-keys/{category}` request is received without `provider` in the body
- **THEN** the system returns HTTP 400 with a validation error message

#### Scenario: Replace an existing provider config for a category
- **WHEN** a `PUT /users/{userId}/api-keys/{category}` request is received for a category that already has a config stored
- **THEN** the system updates the provider, base URL, and encrypted key, replacing the stored value

### Requirement: List API key providers for a user
The system SHALL provide an endpoint to list which provider configurations a user has set up. The response SHALL include the `category`, `provider`, and `baseUrl` (resolved, with default applied if stored value is null) for each config — NOT the actual key values or encrypted values.

#### Scenario: List configs with categories
- **WHEN** a `GET /users/{userId}/api-keys` request is received for a user with an LLM config for "openrouter" and a TTS config for "openai"
- **THEN** the system returns HTTP 200 with a JSON array of `[{"category": "LLM", "provider": "openrouter", "baseUrl": "https://openrouter.ai/api"}, {"category": "TTS", "provider": "openai", "baseUrl": "https://api.openai.com"}]`

#### Scenario: List configs when none configured
- **WHEN** a `GET /users/{userId}/api-keys` request is received for a user with no provider configs
- **THEN** the system returns HTTP 200 with an empty JSON array

#### Scenario: List configs for non-existing user
- **WHEN** a `GET /users/{userId}/api-keys` request is received for a user that does not exist
- **THEN** the system returns HTTP 404

### Requirement: Delete an API key
The system SHALL allow deleting a user's provider configuration for a specific category.

#### Scenario: Delete existing config by category
- **WHEN** a `DELETE /users/{userId}/api-keys/{category}` request is received for a user that has a config for that category
- **THEN** the system deletes the config record and returns HTTP 204

#### Scenario: Delete non-existing config by category
- **WHEN** a `DELETE /users/{userId}/api-keys/{category}` request is received for a category that has no config stored
- **THEN** the system returns HTTP 404

#### Scenario: Delete with invalid category
- **WHEN** a `DELETE /users/{userId}/api-keys/{category}` request is received with a `category` that is not `LLM` or `TTS`
- **THEN** the system returns HTTP 400 with a validation error message

### Requirement: API key resolution with fallback
When the pipeline needs a provider configuration, the system SHALL resolve it by category in this order: (1) load the user's config for that category, decrypt the API key if present, and resolve the base URL (stored value or provider default), (2) fall back to the corresponding environment variable and default base URL (`OPENROUTER_API_KEY` + `https://openrouter.ai/api` for category `"LLM"`, `OPENAI_API_KEY` + `https://api.openai.com` for category `"TTS"`). The resolution SHALL return a `ProviderConfig` containing `baseUrl: String` and `apiKey: String?`. If neither a user config nor env var exists, the pipeline step SHALL fail with a clear error.

#### Scenario: User has a config for the category
- **WHEN** the pipeline runs for a podcast whose owning user has a provider config stored for category `LLM` with provider `"ollama"` and no API key
- **THEN** the system resolves the config with `baseUrl = "http://localhost:11434/v1"` and `apiKey = null`, and uses these for LLM calls

#### Scenario: User has no config, global env var exists
- **WHEN** the pipeline runs for a podcast whose owning user has no `LLM` config, but the `OPENROUTER_API_KEY` environment variable is set
- **THEN** the system resolves the config with `baseUrl = "https://openrouter.ai/api"` and the env var value as `apiKey`

#### Scenario: No config available anywhere
- **WHEN** the pipeline runs for a podcast whose owning user has no `LLM` config and the `OPENROUTER_API_KEY` environment variable is not set
- **THEN** the pipeline step fails with an error indicating no provider config is available for the LLM category

#### Scenario: Spring AI auto-configuration excluded
- **WHEN** the application starts
- **THEN** no Spring AI auto-configured chat or audio beans are created, and no `spring.ai` properties are required

### Requirement: Cascade delete API keys with user
When a user is deleted, all of the user's provider configurations SHALL be deleted as part of the cascade.

#### Scenario: Delete user removes provider configs
- **WHEN** a user with 2 stored provider configs is deleted
- **THEN** both config records are removed from the `user_provider_configs` table

### Requirement: Migrate existing API keys to categories
A Flyway migration SHALL rename the `user_api_keys` table to `user_provider_configs`, add the `base_url` column (nullable), and make `encrypted_api_key` nullable. Existing data SHALL be preserved. The primary key SHALL remain `(user_id, category)`.

#### Scenario: Migration renames table and adds column
- **WHEN** the Flyway migration runs on a database with existing rows in `user_api_keys`
- **THEN** the table is renamed to `user_provider_configs`, the `base_url` column is added (null for all existing rows), `encrypted_api_key` becomes nullable, and all existing data is preserved

#### Scenario: Migration on empty table
- **WHEN** the Flyway migration runs on a database with no rows in `user_api_keys`
- **THEN** the migration completes successfully with the new table structure

## RENAMED Requirements

### Requirement: API keys encrypted at rest
- **FROM:** API keys encrypted at rest
- **TO:** Provider credentials encrypted at rest

## MODIFIED Requirements

### Requirement: Store API key for a provider
The system SHALL store per-user provider configurations in a `user_provider_configs` table with columns: `user_id` (TEXT, FK to users), `provider` (TEXT, NOT NULL), `category` (TEXT, NOT NULL), `base_url` (TEXT, nullable), `encrypted_api_key` (TEXT, nullable, Base64-encoded IV+ciphertext), with a composite primary key of `(user_id, category, provider)`. Supported category values SHALL be `"LLM"` and `"TTS"`. The `provider` field SHALL identify the external service (e.g., `"openrouter"`, `"openai"`, `"ollama"`). The API key SHALL be encrypted before storage and decrypted only when needed at pipeline runtime. The API key MAY be null for providers that do not require authentication (e.g., Ollama). Each user MAY have multiple configurations per category, but SHALL have at most one configuration per category-provider combination.

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

#### Scenario: Replace an existing provider config for a category and provider
- **WHEN** a `PUT /users/{userId}/api-keys/{category}` request is received for a category and provider combination that already has a config stored
- **THEN** the system updates the base URL and encrypted key for that specific provider, leaving other providers in the same category unchanged

#### Scenario: Add a second provider to the same category
- **WHEN** a user already has an LLM config for provider `"openrouter"` and a `PUT /users/{userId}/api-keys/LLM` request is received with provider `"ollama"`
- **THEN** the system stores the new config alongside the existing one, and both configs are returned by the list endpoint

### Requirement: List API key providers for a user
The system SHALL provide an endpoint to list which provider configurations a user has set up. The response SHALL include the `category`, `provider`, and `baseUrl` (resolved, with default applied if stored value is null) for each config — NOT the actual key values or encrypted values. When a user has multiple providers for the same category, all SHALL be returned.

#### Scenario: List configs with multiple providers per category
- **WHEN** a `GET /users/{userId}/api-keys` request is received for a user with two LLM configs (`"openrouter"` and `"ollama"`) and one TTS config (`"openai"`)
- **THEN** the system returns HTTP 200 with a JSON array containing three entries

#### Scenario: List configs when none configured
- **WHEN** a `GET /users/{userId}/api-keys` request is received for a user with no provider configs
- **THEN** the system returns HTTP 200 with an empty JSON array

#### Scenario: List configs for non-existing user
- **WHEN** a `GET /users/{userId}/api-keys` request is received for a user that does not exist
- **THEN** the system returns HTTP 404

### Requirement: Delete an API key
The system SHALL allow deleting a user's provider configuration for a specific category and provider combination. The delete endpoint SHALL target a single provider within a category, not all providers in a category.

#### Scenario: Delete existing config by category and provider
- **WHEN** a `DELETE /users/{userId}/api-keys/{category}/{provider}` request is received for a user that has a config for that category and provider
- **THEN** the system deletes that specific config record and returns HTTP 204, leaving other providers in the same category unchanged

#### Scenario: Delete non-existing config by category and provider
- **WHEN** a `DELETE /users/{userId}/api-keys/{category}/{provider}` request is received for a category and provider combination that has no config stored
- **THEN** the system returns HTTP 404

#### Scenario: Delete with invalid category
- **WHEN** a `DELETE /users/{userId}/api-keys/{category}/{provider}` request is received with a `category` that is not `LLM` or `TTS`
- **THEN** the system returns HTTP 400 with a validation error message

### Requirement: API key resolution with fallback
When the pipeline needs a provider configuration, the system SHALL resolve it by category in this order: (1) load the user's configs for that category, pick the first one found, decrypt the API key if present, and resolve the base URL (stored value or provider default), (2) fall back to the corresponding environment variable and default base URL (`OPENROUTER_API_KEY` + `https://openrouter.ai/api` for category `"LLM"`, `OPENAI_API_KEY` + `https://api.openai.com` for category `"TTS"`). The resolution SHALL return a `ProviderConfig` containing `baseUrl: String` and `apiKey: String?`. If neither a user config nor env var exists, the pipeline step SHALL fail with a clear error.

#### Scenario: User has one config for the category
- **WHEN** the pipeline runs for a podcast whose owning user has a single provider config stored for category `LLM` with provider `"ollama"` and no API key
- **THEN** the system resolves the config with `baseUrl = "http://localhost:11434"` and `apiKey = null`

#### Scenario: User has multiple configs for the category
- **WHEN** the pipeline runs for a podcast whose owning user has two LLM configs (`"openrouter"` and `"ollama"`)
- **THEN** the system resolves using the first stored config for that category

#### Scenario: User has no config, global env var exists
- **WHEN** the pipeline runs for a podcast whose owning user has no `LLM` config, but the `OPENROUTER_API_KEY` environment variable is set
- **THEN** the system resolves the config with `baseUrl = "https://openrouter.ai/api"` and the env var value as `apiKey`

### Requirement: Migrate to composite primary key
A Flyway migration SHALL change the `user_provider_configs` primary key from `(user_id, category)` to `(user_id, category, provider)`. Existing data SHALL be preserved. Since SQLite does not support altering primary keys, the migration SHALL recreate the table with the new key and copy existing data.

#### Scenario: Migration preserves existing data
- **WHEN** the Flyway migration runs on a database with existing rows in `user_provider_configs`
- **THEN** all existing rows are preserved in the new table with the `(user_id, category, provider)` primary key

#### Scenario: Migration on empty table
- **WHEN** the Flyway migration runs on a database with no rows in `user_provider_configs`
- **THEN** the migration completes successfully with the new table structure

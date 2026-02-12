## MODIFIED Requirements

### Requirement: Store API key for a provider
The system SHALL store per-user API keys in a `user_api_keys` table with columns: `user_id` (TEXT, FK to users), `provider` (TEXT), `category` (TEXT, NOT NULL), `encrypted_api_key` (TEXT, Base64-encoded IV+ciphertext), with a composite primary key of `(user_id, category)`. Supported category values SHALL be `"LLM"` and `"TTS"`. The `provider` field SHALL identify the external service (e.g., `"openrouter"`, `"openai"`). The API key SHALL be encrypted before storage and decrypted only when needed at pipeline runtime. Each user SHALL have at most one key per category.

#### Scenario: Set an API key
- **WHEN** a `PUT /users/{userId}/api-keys/{category}` request is received with a JSON body containing `apiKey` and `provider`, and `category` is a valid value (`LLM` or `TTS`)
- **THEN** the system encrypts the API key with AES-256-GCM, stores it with the provider and category in the `user_api_keys` table, and returns HTTP 200

#### Scenario: Set API key with invalid category
- **WHEN** a `PUT /users/{userId}/api-keys/{category}` request is received with a `category` that is not `LLM` or `TTS`
- **THEN** the system returns HTTP 400 with a validation error message indicating the valid categories

#### Scenario: Set API key for non-existing user
- **WHEN** a `PUT /users/{userId}/api-keys/{category}` request is received for a user that does not exist
- **THEN** the system returns HTTP 404

#### Scenario: Set API key with missing value
- **WHEN** a `PUT /users/{userId}/api-keys/{category}` request is received without `apiKey` in the body
- **THEN** the system returns HTTP 400 with a validation error message

#### Scenario: Set API key with missing provider
- **WHEN** a `PUT /users/{userId}/api-keys/{category}` request is received without `provider` in the body
- **THEN** the system returns HTTP 400 with a validation error message

#### Scenario: Replace an existing API key for a category
- **WHEN** a `PUT /users/{userId}/api-keys/{category}` request is received for a category that already has a key stored
- **THEN** the system encrypts the new key, updates the provider and encrypted key, and replaces the stored value

### Requirement: List API key providers for a user
The system SHALL provide an endpoint to list which API keys a user has configured. The response SHALL include the `category` and `provider` for each key — NOT the actual key values or encrypted values.

#### Scenario: List keys with categories
- **WHEN** a `GET /users/{userId}/api-keys` request is received for a user with an LLM key for "openrouter" and a TTS key for "openai"
- **THEN** the system returns HTTP 200 with a JSON array of `[{"category": "LLM", "provider": "openrouter"}, {"category": "TTS", "provider": "openai"}]`

#### Scenario: List keys when none configured
- **WHEN** a `GET /users/{userId}/api-keys` request is received for a user with no API keys
- **THEN** the system returns HTTP 200 with an empty JSON array

#### Scenario: List keys for non-existing user
- **WHEN** a `GET /users/{userId}/api-keys` request is received for a user that does not exist
- **THEN** the system returns HTTP 404

### Requirement: Delete an API key
The system SHALL allow deleting a user's API key for a specific category.

#### Scenario: Delete existing API key by category
- **WHEN** a `DELETE /users/{userId}/api-keys/{category}` request is received for a user that has a key for that category
- **THEN** the system deletes the encrypted key record and returns HTTP 204

#### Scenario: Delete non-existing API key by category
- **WHEN** a `DELETE /users/{userId}/api-keys/{category}` request is received for a category that has no key stored
- **THEN** the system returns HTTP 404

#### Scenario: Delete with invalid category
- **WHEN** a `DELETE /users/{userId}/api-keys/{category}` request is received with a `category` that is not `LLM` or `TTS`
- **THEN** the system returns HTTP 400 with a validation error message

### Requirement: API key resolution with fallback
When the pipeline needs an API key, the system SHALL resolve it by category in this order: (1) decrypt the owning user's key for that category, (2) fall back to the corresponding environment variable (`OPENROUTER_API_KEY` for category `"LLM"`, `OPENAI_API_KEY` for category `"TTS"`). If neither exists, the pipeline step SHALL fail with a clear error. No Spring AI auto-configured beans SHALL be involved in key resolution.

#### Scenario: User has a key for the category
- **WHEN** the pipeline runs for a podcast whose owning user has an encrypted API key stored for category `LLM`
- **THEN** the system decrypts the key and uses it for LLM calls

#### Scenario: User has no key, global env var exists
- **WHEN** the pipeline runs for a podcast whose owning user has no `LLM` API key, but the `OPENROUTER_API_KEY` environment variable is set
- **THEN** the system uses the environment variable value for LLM calls

#### Scenario: No key available anywhere
- **WHEN** the pipeline runs for a podcast whose owning user has no `LLM` API key and the `OPENROUTER_API_KEY` environment variable is not set
- **THEN** the pipeline step fails with an error indicating no API key is available for the LLM category

#### Scenario: Spring AI auto-configuration excluded
- **WHEN** the application starts
- **THEN** no Spring AI auto-configured chat or audio beans are created, and no `spring.ai` properties are required

## ADDED Requirements

### Requirement: Migrate existing API keys to categories
A Flyway migration SHALL add the `category` column to the `user_api_keys` table and backfill existing rows: provider `"openrouter"` SHALL be assigned category `"LLM"`, provider `"openai"` SHALL be assigned category `"TTS"`. The migration SHALL change the primary key from `(user_id, provider)` to `(user_id, category)`.

#### Scenario: Migration backfills existing keys
- **WHEN** the Flyway migration runs on a database with existing API keys for providers `"openrouter"` and `"openai"`
- **THEN** the keys are assigned categories `"LLM"` and `"TTS"` respectively, and the primary key is updated to `(user_id, category)`

#### Scenario: Migration on empty table
- **WHEN** the Flyway migration runs on a database with no rows in `user_api_keys`
- **THEN** the migration completes successfully, adding the `category` column and updating the primary key

## ADDED Requirements

### Requirement: API keys encrypted at rest
The system SHALL encrypt API keys before storing them in the database using AES-256-GCM. Each key SHALL be encrypted with a unique random IV (initialization vector). The combined `IV + ciphertext` SHALL be Base64-encoded and stored in the `encrypted_api_key` column. The master encryption key SHALL be provided via the `app.encryption.master-key` application property.

#### Scenario: Master key configured
- **WHEN** the application starts with `app.encryption.master-key` set (e.g., via `APP_ENCRYPTION_MASTER_KEY` environment variable)
- **THEN** the application starts successfully and can encrypt/decrypt API keys

#### Scenario: Master key not configured
- **WHEN** the application starts without `app.encryption.master-key` configured
- **THEN** the application fails to start with a clear error message indicating the master encryption key is required

#### Scenario: Each key has unique IV
- **WHEN** two API keys are stored for the same provider by different users
- **THEN** each stored value uses a different random IV, producing different ciphertext even if the API keys happen to be identical

### Requirement: Store API key for a provider
The system SHALL store per-user API keys in a `user_api_keys` table with columns: `user_id` (TEXT, FK to users), `provider` (TEXT), `encrypted_api_key` (TEXT, Base64-encoded IV+ciphertext), with a composite primary key of `(user_id, provider)`. Supported provider values SHALL include `"openrouter"` and `"openai"`. The API key SHALL be encrypted before storage and decrypted only when needed at pipeline runtime.

#### Scenario: Set an API key
- **WHEN** a `PUT /users/{userId}/api-keys/{provider}` request is received with a JSON body containing `apiKey`
- **THEN** the system encrypts the API key with AES-256-GCM, stores it in the `encrypted_api_key` column, and returns HTTP 200

#### Scenario: Set API key for non-existing user
- **WHEN** a `PUT /users/{userId}/api-keys/{provider}` request is received for a user that does not exist
- **THEN** the system returns HTTP 404

#### Scenario: Set API key with missing value
- **WHEN** a `PUT /users/{userId}/api-keys/{provider}` request is received without `apiKey` in the body
- **THEN** the system returns HTTP 400 with a validation error message

#### Scenario: Replace an existing API key
- **WHEN** a `PUT /users/{userId}/api-keys/{provider}` request is received for a provider that already has a key stored
- **THEN** the system encrypts the new key and replaces the stored value

### Requirement: List API key providers for a user
The system SHALL provide an endpoint to list which providers a user has configured API keys for. The response SHALL include provider names only — NOT the actual key values or encrypted values.

#### Scenario: List providers with keys
- **WHEN** a `GET /users/{userId}/api-keys` request is received for a user with keys for "openrouter" and "openai"
- **THEN** the system returns HTTP 200 with a JSON array of `[{"provider": "openrouter"}, {"provider": "openai"}]`

#### Scenario: List providers when none configured
- **WHEN** a `GET /users/{userId}/api-keys` request is received for a user with no API keys
- **THEN** the system returns HTTP 200 with an empty JSON array

#### Scenario: List providers for non-existing user
- **WHEN** a `GET /users/{userId}/api-keys` request is received for a user that does not exist
- **THEN** the system returns HTTP 404

### Requirement: Delete an API key
The system SHALL allow deleting a user's API key for a specific provider.

#### Scenario: Delete existing API key
- **WHEN** a `DELETE /users/{userId}/api-keys/{provider}` request is received for a user that has a key for that provider
- **THEN** the system deletes the encrypted key record and returns HTTP 204

#### Scenario: Delete non-existing API key
- **WHEN** a `DELETE /users/{userId}/api-keys/{provider}` request is received for a provider that has no key stored
- **THEN** the system returns HTTP 404

### Requirement: API key resolution with fallback
When the pipeline needs an API key for a provider, the system SHALL resolve it in this order: (1) decrypt the owning user's key for that provider, (2) the global API key from application config. If neither exists, the pipeline step SHALL fail with a clear error.

#### Scenario: User has a key for the provider
- **WHEN** the pipeline runs for a podcast whose owning user has an encrypted "openrouter" API key stored
- **THEN** the system decrypts the key and uses it for LLM calls

#### Scenario: User has no key, global key exists
- **WHEN** the pipeline runs for a podcast whose owning user has no "openrouter" API key, but the global application config has an OpenRouter API key
- **THEN** the system uses the global API key for LLM calls

#### Scenario: No key available anywhere
- **WHEN** the pipeline runs for a podcast whose owning user has no "openrouter" API key and no global key is configured
- **THEN** the pipeline step fails with an error indicating no API key is available for the provider

### Requirement: Cascade delete API keys with user
When a user is deleted, all of the user's encrypted API keys SHALL be deleted as part of the cascade.

#### Scenario: Delete user removes API keys
- **WHEN** a user with 2 stored API keys is deleted
- **THEN** both API key records are removed from the `user_api_keys` table

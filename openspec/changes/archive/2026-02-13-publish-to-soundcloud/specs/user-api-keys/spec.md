## ADDED Requirements

### Requirement: OAuth connections are separate from API key configs
The system SHALL store OAuth-based external service connections (e.g., SoundCloud) in a separate `oauth_connections` table, not in the `user_provider_configs` table. The existing API key resolution, provider defaults, and fallback mechanisms SHALL remain unchanged and SHALL NOT interact with OAuth connections.

#### Scenario: OAuth connection does not appear in API key listing
- **WHEN** a `GET /users/{userId}/api-keys` request is received for a user who has a SoundCloud OAuth connection
- **THEN** the SoundCloud connection is NOT included in the response — only `user_provider_configs` entries are returned

#### Scenario: Existing API key operations unaffected
- **WHEN** a user has both API key configs (LLM, TTS) and an OAuth connection (SoundCloud)
- **THEN** all existing API key CRUD operations and pipeline resolution work exactly as before

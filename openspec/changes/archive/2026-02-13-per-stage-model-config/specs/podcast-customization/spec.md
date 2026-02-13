## MODIFIED Requirements

### Requirement: LLM model selection per podcast
Each podcast SHALL have an optional `llm_models` field (TEXT, nullable, stored as JSON). The JSON value SHALL be a map of stage name to named model reference (e.g., `{"filter": "local", "compose": "capable"}`). When a stage key is present, the LLM pipeline SHALL use the referenced named model for that stage. When a stage key is absent or `llm_models` is null, the system SHALL fall back to the global stage default from `app.llm.defaults`. The `llm_models` field SHALL be serialized to/from JSON using a custom Spring Data JDBC converter.

#### Scenario: Podcast with per-stage model overrides
- **WHEN** a podcast has `llm_models` set to `{"filter": "local", "compose": "capable"}`
- **THEN** the filter stage uses the "local" model and the compose stage uses the "capable" model

#### Scenario: Podcast with partial override
- **WHEN** a podcast has `llm_models` set to `{"compose": "local"}`
- **THEN** the compose stage uses the "local" model and the filter stage falls back to the global default

#### Scenario: Podcast with no LLM model overrides
- **WHEN** a podcast has `llm_models` set to null
- **THEN** both stages use their global defaults from `app.llm.defaults`

#### Scenario: Podcast with empty map
- **WHEN** a podcast has `llm_models` set to `{}`
- **THEN** both stages use their global defaults from `app.llm.defaults`

### Requirement: Customization fields in podcast CRUD
All customization fields SHALL be accepted as optional fields in the podcast create (`POST`) and update (`PUT`) endpoints. The API response for a podcast SHALL include all customization fields with their effective values (stored value or default). The `llmModels` field SHALL be accepted and returned as a JSON object mapping stage names to model names (e.g., `{"filter": "cheap", "compose": "capable"}`). The old `llmModel` field SHALL no longer be accepted.

#### Scenario: Create podcast with per-stage model config
- **WHEN** a `POST /users/{userId}/podcasts` request includes `llmModels: {"compose": "local"}`
- **THEN** the podcast is created with `llm_models` stored as `{"compose": "local"}`

#### Scenario: Update podcast model config
- **WHEN** a `PUT /users/{userId}/podcasts/{podcastId}` request includes `llmModels: {"filter": "local", "compose": "capable"}`
- **THEN** the podcast's `llm_models` is updated to the new value

#### Scenario: Get podcast includes model config
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}` request is received
- **THEN** the response includes `llmModels` (the JSON map, or null if not set) along with all other customization fields

#### Scenario: Create podcast with customization
- **WHEN** a `POST /users/{userId}/podcasts` request includes `name`, `topic`, `ttsVoice: "alloy"`, `style: "casual"`, `language: "fr"`, and `cron: "0 0 8 * * MON-FRI"`
- **THEN** the podcast is created with the specified values and defaults for unspecified fields

#### Scenario: Update podcast customization
- **WHEN** a `PUT /users/{userId}/podcasts/{podcastId}` request includes `llmModels: {"compose": "local"}`
- **THEN** the podcast's `llm_models` is updated and other fields remain unchanged

#### Scenario: Get podcast includes customization
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}` request is received
- **THEN** the response includes all customization fields (llmModels, ttsVoice, ttsSpeed, style, targetWords, cron, customInstructions, language)

## REMOVED Requirements

### Requirement: LLM model selection per podcast
**Reason**: Replaced by the modified version above. The single `llm_model` field is replaced by the `llm_models` JSON map supporting per-stage overrides.
**Migration**: Existing `llm_model` values are migrated to `llm_models` as `{"filter": "<value>", "compose": "<value>"}`. The API field changes from `llmModel: String?` to `llmModels: Map<String, String>?`.

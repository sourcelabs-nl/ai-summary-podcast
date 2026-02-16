## MODIFIED Requirements

### Requirement: Customization fields in podcast CRUD
All customization fields SHALL be accepted as optional fields in the podcast create (`POST`) and update (`PUT`) endpoints. The API response for a podcast SHALL include all customization fields with their effective values (stored value or default). The `llmModels` field SHALL be accepted and returned as a JSON object mapping stage names to model names (e.g., `{"filter": "cheap", "compose": "capable"}`). The old `llmModel` field SHALL no longer be accepted. All nullable primitive-typed DTO fields (`Int?`, `Boolean?`, `Double?`) SHALL use Jackson 3 `@JsonProperty` annotations to ensure correct deserialization.

The `maxLlmCostCents` field SHALL be accepted as an optional nullable integer in the podcast create (`POST`) and update (`PUT`) endpoints. When set, it overrides the global `app.llm.max-cost-cents` threshold for that podcast's LLM pipeline cost gate. When null, the global default applies. The field SHALL be included in the podcast GET response.

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
- **THEN** the response includes all customization fields (llmModels, ttsVoice, ttsSpeed, style, targetWords, cron, customInstructions, language, maxLlmCostCents)

#### Scenario: Create podcast with cost threshold
- **WHEN** a `POST /users/{userId}/podcasts` request includes `"maxLlmCostCents": 500`
- **THEN** the podcast is created with `max_llm_cost_cents` set to 500

#### Scenario: Update podcast cost threshold
- **WHEN** a `PUT /users/{userId}/podcasts/{podcastId}` request includes `"maxLlmCostCents": 300`
- **THEN** the podcast's `max_llm_cost_cents` is updated to 300

#### Scenario: Create podcast without cost threshold
- **WHEN** a `POST /users/{userId}/podcasts` request does not include `maxLlmCostCents`
- **THEN** the podcast is created with `max_llm_cost_cents` as null (uses global default)

#### Scenario: Get podcast includes cost threshold
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}` request is received for a podcast with `max_llm_cost_cents` set to 500
- **THEN** the response includes `"maxLlmCostCents": 500`

## MODIFIED Requirements

### Requirement: Relevance threshold per podcast
Each podcast SHALL have a `relevance_threshold` field (INTEGER, NOT NULL, default 5). The LLM pipeline SHALL use this threshold to determine which scored articles are relevant: articles with `relevance_score >= relevance_threshold` are considered relevant. Valid values are 0-10. The field SHALL be accepted in podcast create (`POST`) and update (`PUT`) endpoints and included in GET responses. Jackson 3 `@JsonProperty` annotations SHALL be used on the `relevanceThreshold` DTO field to ensure correct deserialization of nullable `Int?` values.

#### Scenario: Create podcast with custom relevance threshold
- **WHEN** a `POST /users/{userId}/podcasts` request includes `"relevanceThreshold": 3`
- **THEN** the system creates the podcast with `relevance_threshold` set to 3 and the response body SHALL contain `"relevanceThreshold": 3`

#### Scenario: Update podcast relevance threshold
- **WHEN** a `PUT /users/{userId}/podcasts/{podcastId}` request includes `"relevanceThreshold": 8`
- **THEN** the podcast's `relevance_threshold` is updated to 8 and the response body SHALL contain `"relevanceThreshold": 8`

#### Scenario: Podcast with default relevance threshold
- **WHEN** a podcast is created without specifying `relevance_threshold`
- **THEN** the `relevance_threshold` defaults to 5

### Requirement: Customization fields in podcast CRUD
All customization fields SHALL be accepted as optional fields in the podcast create (`POST`) and update (`PUT`) endpoints. The API response for a podcast SHALL include all customization fields with their effective values (stored value or default). The `llmModels` field SHALL be accepted and returned as a JSON object mapping stage names to model names (e.g., `{"filter": "cheap", "compose": "capable"}`). All nullable primitive-typed DTO fields (`Int?`, `Boolean?`, `Double?`) SHALL use Jackson 3 `@JsonProperty` annotations to ensure correct deserialization.

#### Scenario: Create podcast with all customization fields
- **WHEN** a `POST /users/{userId}/podcasts` request includes `name`, `topic`, `ttsVoice: "alloy"`, `ttsSpeed: 1.25`, `style: "casual"`, `targetWords: 800`, `relevanceThreshold: 3`, `requireReview: true`, and `cron: "0 0 8 * * MON-FRI"`
- **THEN** the podcast is created with all specified values and the response body SHALL reflect each provided value accurately

#### Scenario: Update podcast customization fields
- **WHEN** a `PUT /users/{userId}/podcasts/{podcastId}` request includes `relevanceThreshold: 8` and `requireReview: true`
- **THEN** the podcast's `relevance_threshold` and `require_review` are updated and the response body reflects the new values

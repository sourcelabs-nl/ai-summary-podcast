## MODIFIED Requirements

### Requirement: Customization fields in podcast CRUD
All customization fields SHALL be accepted as optional fields in the podcast create (`POST`) and update (`PUT`) endpoints. The API response for a podcast SHALL include all customization fields with their effective values (stored value or default). The `llmModels` field SHALL be accepted and returned as a JSON object mapping stage names to model names (e.g., `{"filter": "cheap", "compose": "capable"}`). The old `llmModel` field SHALL no longer be accepted. All nullable primitive-typed DTO fields (`Int?`, `Boolean?`, `Double?`) SHALL use Jackson 3 `@JsonProperty` annotations to ensure correct deserialization.

The `maxLlmCostCents` field SHALL be accepted as an optional nullable integer in the podcast create (`POST`) and update (`PUT`) endpoints. When set, it overrides the global `app.llm.max-cost-cents` threshold for that podcast's LLM pipeline cost gate. When null, the global default applies. The field SHALL be included in the podcast GET response.

The `maxArticleAgeDays` field SHALL be accepted as an optional nullable integer in the podcast create (`POST`) and update (`PUT`) endpoints. When set, it overrides the global `app.source.max-article-age-days` for that podcast. When null, the global default applies. The field SHALL be included in the podcast GET response.

#### Scenario: Create podcast with custom article age
- **WHEN** a `POST /users/{userId}/podcasts` request includes `"maxArticleAgeDays": 14`
- **THEN** the podcast is created with `max_article_age_days` set to 14

#### Scenario: Update podcast article age
- **WHEN** a `PUT /users/{userId}/podcasts/{podcastId}` request includes `"maxArticleAgeDays": 1`
- **THEN** the podcast's `max_article_age_days` is updated to 1

#### Scenario: Create podcast without article age
- **WHEN** a `POST /users/{userId}/podcasts` request does not include `maxArticleAgeDays`
- **THEN** the podcast is created with `max_article_age_days` as null (uses global default of 7)

#### Scenario: Get podcast includes article age
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}` request is received for a podcast with `max_article_age_days` set to 14
- **THEN** the response includes `"maxArticleAgeDays": 14`

#### Scenario: Get podcast includes customization
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}` request is received
- **THEN** the response includes all customization fields (llmModels, ttsVoice, ttsSpeed, style, targetWords, cron, customInstructions, language, maxLlmCostCents, maxArticleAgeDays)

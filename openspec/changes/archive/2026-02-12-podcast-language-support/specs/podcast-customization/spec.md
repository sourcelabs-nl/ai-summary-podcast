## ADDED Requirements

### Requirement: Language selection per podcast
Each podcast SHALL have a `language` field (TEXT, NOT NULL, default `"en"`). The value MUST be a valid ISO 639-1 code from the set of supported languages. The language controls the briefing script language, date formatting locale, and RSS feed language metadata.

#### Scenario: Podcast with custom language
- **WHEN** a podcast has `language` set to `"nl"`
- **THEN** the briefing composer writes the script in Dutch and formats dates using the Dutch locale

#### Scenario: Podcast with default language
- **WHEN** a podcast is created without specifying `language`
- **THEN** the `language` defaults to `"en"` (English)

#### Scenario: Invalid language rejected on create
- **WHEN** a `POST /users/{userId}/podcasts` request includes `language: "xx"`
- **THEN** the system returns HTTP 400 with an error indicating the language is not supported

#### Scenario: Invalid language rejected on update
- **WHEN** a `PUT /users/{userId}/podcasts/{podcastId}` request includes `language: "xx"`
- **THEN** the system returns HTTP 400 with an error indicating the language is not supported

## MODIFIED Requirements

### Requirement: Customization fields in podcast CRUD
All customization fields SHALL be accepted as optional fields in the podcast create (`POST`) and update (`PUT`) endpoints. The API response for a podcast SHALL include all customization fields with their effective values (stored value or default).

#### Scenario: Create podcast with customization
- **WHEN** a `POST /users/{userId}/podcasts` request includes `name`, `topic`, `ttsVoice: "alloy"`, `style: "casual"`, `language: "fr"`, and `cron: "0 0 8 * * MON-FRI"`
- **THEN** the podcast is created with the specified values and defaults for unspecified fields

#### Scenario: Update podcast customization
- **WHEN** a `PUT /users/{userId}/podcasts/{podcastId}` request includes `llmModel: "openai/gpt-4o"`
- **THEN** the podcast's `llm_model` is updated and other fields remain unchanged

#### Scenario: Get podcast includes customization
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}` request is received
- **THEN** the response includes all customization fields (llmModel, ttsVoice, ttsSpeed, style, targetWords, cron, customInstructions, language)

# Capability: Podcast Customization

## Purpose

Per-podcast customization of LLM model, TTS voice/speed, briefing style, target word count, custom instructions, and generation schedule.

## Requirements

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

### Requirement: TTS voice selection per podcast
Each podcast SHALL have a `tts_voice` field (TEXT, default `"nova"`). The TTS pipeline SHALL use this voice when generating audio for the podcast. Valid values are OpenAI TTS voices: `alloy`, `echo`, `fable`, `onyx`, `nova`, `shimmer`.

#### Scenario: Podcast with custom voice
- **WHEN** a podcast has `tts_voice` set to `"alloy"`
- **THEN** the TTS pipeline generates audio using the "alloy" voice

#### Scenario: Podcast with default voice
- **WHEN** a podcast is created without specifying `tts_voice`
- **THEN** the `tts_voice` defaults to `"nova"`

### Requirement: TTS speed selection per podcast
Each podcast SHALL have a `tts_speed` field (REAL, default `1.0`). The TTS pipeline SHALL use this speed multiplier when generating audio. Valid range is `0.25` to `4.0`.

#### Scenario: Podcast with faster speed
- **WHEN** a podcast has `tts_speed` set to `1.25`
- **THEN** the TTS pipeline generates audio at 1.25x speed

#### Scenario: Podcast with default speed
- **WHEN** a podcast is created without specifying `tts_speed`
- **THEN** the `tts_speed` defaults to `1.0`

### Requirement: Briefing style selection per podcast
Each podcast SHALL have a `style` field (TEXT, default `"news-briefing"`). The briefing composer SHALL adapt its system prompt based on the selected style. Supported styles:
- `news-briefing` — professional news anchor tone, structured with transitions
- `casual` — conversational, relaxed, like chatting with a friend
- `deep-dive` — analytical, in-depth exploration
- `executive-summary` — concise, fact-focused, minimal commentary

#### Scenario: Podcast with casual style
- **WHEN** a podcast has `style` set to `"casual"`
- **THEN** the briefing composer uses a conversational, relaxed tone in its system prompt

#### Scenario: Podcast with default style
- **WHEN** a podcast is created without specifying `style`
- **THEN** the `style` defaults to `"news-briefing"`

### Requirement: Target word count per podcast
Each podcast SHALL have an optional `target_words` field (INTEGER, nullable). When set, the briefing composer SHALL target this word count for the script. When null, the system SHALL fall back to the global `app.briefing.targetWords` config value.

#### Scenario: Podcast with custom target words
- **WHEN** a podcast has `target_words` set to `800`
- **THEN** the briefing composer targets approximately 800 words for the script

#### Scenario: Podcast with no target words set
- **WHEN** a podcast has `target_words` set to null
- **THEN** the briefing composer uses the global `app.briefing.targetWords` value (1500)

### Requirement: Custom instructions per podcast
Each podcast SHALL have an optional `custom_instructions` field (TEXT, nullable). When set, the briefing composer SHALL append these instructions to its prompt, allowing free-form customization of the output.

#### Scenario: Podcast with custom instructions
- **WHEN** a podcast has `custom_instructions` set to `"Focus on practical implications and use Dutch language"`
- **THEN** the briefing composer appends these instructions to the system prompt

#### Scenario: Podcast with no custom instructions
- **WHEN** a podcast has `custom_instructions` set to null
- **THEN** the briefing composer uses only its default prompt without additional instructions

### Requirement: Generation schedule per podcast
Each podcast SHALL have a `cron` field (TEXT, default `"0 0 6 * * *"`). The briefing generation scheduler SHALL use this cron expression to determine when to generate a new episode for the podcast. The podcast SHALL also have a `last_generated_at` field (TEXT, nullable) to track the last generation time.

#### Scenario: Podcast with custom schedule
- **WHEN** a podcast has `cron` set to `"0 0 8 * * MON-FRI"` (weekdays at 08:00)
- **THEN** the scheduler generates episodes for this podcast only on weekdays at 08:00

#### Scenario: Podcast with default schedule
- **WHEN** a podcast is created without specifying `cron`
- **THEN** the `cron` defaults to `"0 0 6 * * *"` (daily at 06:00)

#### Scenario: Scheduler respects per-podcast cron
- **WHEN** the scheduler runs and podcast A has cron `"0 0 6 * * *"` (daily 06:00) and podcast B has cron `"0 0 18 * * *"` (daily 18:00), and it is currently 06:05
- **THEN** the scheduler triggers generation for podcast A but not podcast B

#### Scenario: Scheduler tracks last generation time
- **WHEN** the scheduler successfully generates an episode for a podcast
- **THEN** the podcast's `last_generated_at` is updated to the current timestamp

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

### Requirement: Relevance threshold per podcast
Each podcast SHALL have a `relevance_threshold` field (INTEGER, NOT NULL, default 5). The LLM pipeline SHALL use this threshold to determine which scored articles are relevant: articles with `relevance_score >= relevance_threshold` are considered relevant. Valid values are 0-10. The field SHALL be accepted in podcast create (`POST`) and update (`PUT`) endpoints and included in GET responses.

#### Scenario: Podcast with custom relevance threshold
- **WHEN** a podcast has `relevance_threshold` set to 7
- **THEN** only articles with `relevance_score` >= 7 are considered relevant for summarization and briefing composition

#### Scenario: Podcast with default relevance threshold
- **WHEN** a podcast is created without specifying `relevance_threshold`
- **THEN** the `relevance_threshold` defaults to 5

#### Scenario: Strict podcast filters aggressively
- **WHEN** a podcast has `relevance_threshold` set to 8 and 10 articles are scored with scores [2, 3, 5, 6, 7, 7, 8, 8, 9, 10]
- **THEN** only the 4 articles with scores 8, 8, 9, 10 are considered relevant

#### Scenario: Broad podcast includes more articles
- **WHEN** a podcast has `relevance_threshold` set to 3 and 10 articles are scored with scores [2, 3, 5, 6, 7, 7, 8, 8, 9, 10]
- **THEN** 9 articles with scores >= 3 are considered relevant

#### Scenario: Relevance threshold included in API response
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}` request is received
- **THEN** the response includes `relevanceThreshold` with its current value

#### Scenario: Relevance threshold updated via API
- **WHEN** a `PUT /users/{userId}/podcasts/{podcastId}` request includes `relevanceThreshold: 8`
- **THEN** the podcast's `relevance_threshold` is updated to 8

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

### Requirement: TTS provider selection per podcast
Each podcast SHALL have a `tts_provider` field (TEXT, NOT NULL, default `"openai"`). The field SHALL determine which TTS service is used for audio generation. Supported values: `"openai"`, `"elevenlabs"`. The field SHALL be accepted in podcast create (`POST`) and update (`PUT`) endpoints and included in GET responses.

#### Scenario: Podcast with ElevenLabs provider
- **WHEN** a podcast has `tts_provider` set to `"elevenlabs"`
- **THEN** the TTS pipeline uses the ElevenLabs provider for audio generation

#### Scenario: Podcast with default provider
- **WHEN** a podcast is created without specifying `tts_provider`
- **THEN** the `tts_provider` defaults to `"openai"`

#### Scenario: Invalid provider rejected
- **WHEN** a podcast create or update request includes `tts_provider: "azure"`
- **THEN** the system returns HTTP 400 with an error listing supported providers

### Requirement: TTS voice selection per podcast
Each podcast SHALL have a `tts_voices` field (TEXT, nullable, stored as JSON map). The map keys represent speaker roles and values represent voice identifiers. For monologue styles: `{"default": "nova"}`. For dialogue style: `{"host": "voice_id", "cohost": "voice_id"}`. When null, the system SHALL use a default of `{"default": "nova"}`. The `tts_voices` field SHALL be serialized to/from JSON using a custom Spring Data JDBC converter.

#### Scenario: Podcast with single monologue voice
- **WHEN** a podcast has `tts_voices` set to `{"default": "alloy"}`
- **THEN** the TTS pipeline uses the "alloy" voice for monologue generation

#### Scenario: Podcast with dialogue voices
- **WHEN** a podcast has `tts_voices` set to `{"host": "id1", "cohost": "id2"}`
- **THEN** the TTS pipeline maps host lines to voice id1 and cohost lines to voice id2

#### Scenario: Podcast with default voices
- **WHEN** a podcast is created without specifying `tts_voices`
- **THEN** the system uses `{"default": "nova"}` as the default

### Requirement: TTS settings per podcast
Each podcast SHALL have a `tts_settings` field (TEXT, nullable, stored as JSON map). The map contains provider-specific settings (e.g., `{"speed": 1.25}` for OpenAI, `{"stability": 0.5, "similarity_boost": 0.8}` for ElevenLabs). When null, provider defaults apply. The `tts_settings` field SHALL be serialized to/from JSON using a custom Spring Data JDBC converter.

#### Scenario: Podcast with OpenAI speed setting
- **WHEN** a podcast has `tts_settings: {"speed": 1.5}`
- **THEN** the OpenAI TTS provider uses speed 1.5

#### Scenario: Podcast with ElevenLabs stability setting
- **WHEN** a podcast has `tts_settings: {"stability": 0.3}`
- **THEN** the ElevenLabs TTS provider applies stability 0.3

#### Scenario: Podcast with no TTS settings
- **WHEN** a podcast is created without specifying `tts_settings`
- **THEN** the TTS provider uses its default settings

### Requirement: Briefing style selection per podcast
Each podcast SHALL have a `style` field (TEXT, default `"news-briefing"`). The briefing composer SHALL adapt its system prompt based on the selected style. Supported styles:
- `news-briefing` — professional news anchor tone, structured with transitions
- `casual` — conversational, relaxed, like chatting with a friend
- `deep-dive` — analytical, in-depth exploration
- `executive-summary` — concise, fact-focused, minimal commentary
- `dialogue` — multi-speaker conversation (requires ElevenLabs TTS provider)
- `interview` — interviewer/expert conversation with asymmetric roles (requires ElevenLabs TTS provider)

#### Scenario: Dialogue style accepted
- **WHEN** a podcast create request includes `style: "dialogue"`
- **THEN** the podcast is created successfully with the dialogue style

#### Scenario: Interview style accepted
- **WHEN** a podcast create request includes `style: "interview"`, `ttsProvider: "elevenlabs"`, and `ttsVoices: {"interviewer": "id1", "expert": "id2"}`
- **THEN** the podcast is created successfully with the interview style

#### Scenario: Interview style requires ElevenLabs provider
- **WHEN** a podcast create request includes `style: "interview"` and `ttsProvider: "openai"`
- **THEN** the system returns HTTP 400 indicating interview style requires ElevenLabs provider

#### Scenario: Interview style requires exactly interviewer and expert roles
- **WHEN** a podcast create request includes `style: "interview"` and `ttsVoices: {"host": "id1", "cohost": "id2"}`
- **THEN** the system returns HTTP 400 indicating interview style requires exactly `interviewer` and `expert` voice roles

#### Scenario: Interview style requires two voices
- **WHEN** a podcast create request includes `style: "interview"` and `ttsVoices: {"interviewer": "id1"}`
- **THEN** the system returns HTTP 400 indicating interview style requires at least two voice roles

#### Scenario: Dialogue style requires ElevenLabs provider
- **WHEN** a podcast create request includes `style: "dialogue"` and `tts_provider: "openai"`
- **THEN** the system returns HTTP 400 indicating dialogue style requires ElevenLabs provider

#### Scenario: Dialogue style requires multiple voices
- **WHEN** a podcast create request includes `style: "dialogue"` and `tts_voices: {"default": "nova"}`
- **THEN** the system returns HTTP 400 indicating dialogue style requires at least two voice roles (e.g., host and cohost)

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
All customization fields SHALL be accepted as optional fields in the podcast create (`POST`) and update (`PUT`) endpoints. The API response for a podcast SHALL include all customization fields with their effective values (stored value or default). The `llmModels` field SHALL be accepted and returned as a JSON object mapping stage names to model names (e.g., `{"filter": "cheap", "compose": "capable"}`). The old `llmModel` field SHALL no longer be accepted. All nullable primitive-typed DTO fields (`Int?`, `Boolean?`, `Double?`) SHALL use Jackson 3 `@JsonProperty` annotations to ensure correct deserialization.

The `maxLlmCostCents` field SHALL be accepted as an optional nullable integer in the podcast create (`POST`) and update (`PUT`) endpoints. When set, it overrides the global `app.llm.max-cost-cents` threshold for that podcast's LLM pipeline cost gate. When null, the global default applies. The field SHALL be included in the podcast GET response.

The `ttsProvider`, `ttsVoices`, and `ttsSettings` fields SHALL be accepted in podcast create and update endpoints and included in GET responses. The `ttsVoices` and `ttsSettings` fields SHALL be returned as JSON objects.

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
- **WHEN** a `POST /users/{userId}/podcasts` request includes `name`, `topic`, `ttsProvider: "openai"`, `ttsVoices: {"default": "alloy"}`, `style: "casual"`, `language: "fr"`, and `cron: "0 0 8 * * MON-FRI"`
- **THEN** the podcast is created with the specified values and defaults for unspecified fields

#### Scenario: Update podcast customization
- **WHEN** a `PUT /users/{userId}/podcasts/{podcastId}` request includes `llmModels: {"compose": "local"}`
- **THEN** the podcast's `llm_models` is updated and other fields remain unchanged

#### Scenario: Get podcast includes customization
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}` request is received
- **THEN** the response includes all customization fields (llmModels, ttsProvider, ttsVoices, ttsSettings, style, targetWords, cron, customInstructions, language, maxLlmCostCents)

#### Scenario: Create podcast with TTS provider config
- **WHEN** a `POST /users/{userId}/podcasts` request includes `ttsProvider: "elevenlabs"`, `ttsVoices: {"host": "id1", "cohost": "id2"}`, and `ttsSettings: {"stability": 0.5}`
- **THEN** the podcast is created with the specified TTS configuration

#### Scenario: Update podcast TTS provider
- **WHEN** a `PUT /users/{userId}/podcasts/{podcastId}` request includes `ttsProvider: "elevenlabs"`
- **THEN** the podcast's TTS provider is updated to ElevenLabs

#### Scenario: Get podcast includes TTS config
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}` request is received
- **THEN** the response includes `ttsProvider`, `ttsVoices`, and `ttsSettings` fields

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

#### Scenario: Strict podcast filters aggressively
- **WHEN** a podcast has `relevance_threshold` set to 8 and 10 articles are scored with scores [2, 3, 5, 6, 7, 7, 8, 8, 9, 10]
- **THEN** only the 4 articles with scores 8, 8, 9, 10 are considered relevant

#### Scenario: Broad podcast includes more articles
- **WHEN** a podcast has `relevance_threshold` set to 3 and 10 articles are scored with scores [2, 3, 5, 6, 7, 7, 8, 8, 9, 10]
- **THEN** 9 articles with scores >= 3 are considered relevant

#### Scenario: Relevance threshold included in API response
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}` request is received
- **THEN** the response includes `relevanceThreshold` with its current value

### Requirement: Speaker names per podcast
Each podcast SHALL have a `speaker_names` field (TEXT, nullable, stored as JSON map). The map keys SHALL match the keys in `ttsVoices` and values SHALL be display names for speakers (e.g., `{"interviewer": "Alice", "expert": "Bob"}` or `{"host": "Sarah", "cohost": "Mike"}`). When provided, composers SHALL use these names in the generated script so speakers address each other naturally. When null, composers SHALL use role keys or omit names. The `speaker_names` field SHALL be serialized to/from JSON using the same custom Spring Data JDBC converter pattern used for other JSON map fields.

#### Scenario: Podcast with speaker names for interview style
- **WHEN** a podcast has `style: "interview"`, `ttsVoices: {"interviewer": "id1", "expert": "id2"}`, and `speakerNames: {"interviewer": "Alice", "expert": "Bob"}`
- **THEN** the interview composer uses "Alice" and "Bob" as display names in the generated script

#### Scenario: Podcast with speaker names for dialogue style
- **WHEN** a podcast has `style: "dialogue"`, `ttsVoices: {"host": "id1", "cohost": "id2"}`, and `speakerNames: {"host": "Sarah", "cohost": "Mike"}`
- **THEN** the dialogue composer uses "Sarah" and "Mike" as display names in the generated script

#### Scenario: Podcast without speaker names
- **WHEN** a podcast has no `speakerNames` set
- **THEN** composers use role keys as speaker identifiers or omit names (existing behavior)

#### Scenario: Speaker names accepted in create endpoint
- **WHEN** a `POST /users/{userId}/podcasts` request includes `speakerNames: {"host": "Alice", "cohost": "Bob"}`
- **THEN** the podcast is created with the specified speaker names
- **AND** `PodcastService.create()` propagates `speakerNames` to the persisted entity

#### Scenario: Speaker names accepted in update endpoint
- **WHEN** a `PUT /users/{userId}/podcasts/{podcastId}` request includes `speakerNames: {"interviewer": "Alice", "expert": "Bob"}`
- **THEN** the podcast's speaker names are updated
- **AND** `PodcastService.update()` propagates `speakerNames` to the persisted entity

#### Scenario: Speaker names included in GET response
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}` request is received for a podcast with `speakerNames`
- **THEN** the response includes the `speakerNames` field

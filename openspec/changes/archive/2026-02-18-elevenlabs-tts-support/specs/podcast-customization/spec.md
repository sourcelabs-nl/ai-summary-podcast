## MODIFIED Requirements

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

## REMOVED Requirements

### Requirement: TTS speed selection per podcast
**Reason**: Replaced by the `tts_settings` JSON map which supports provider-specific settings including speed.
**Migration**: Existing `tts_speed` values are migrated to `tts_settings: {"speed": <value>}`. The `tts_speed` column is dropped.

## ADDED Requirements

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

### Requirement: Dialogue as a podcast style
The `style` field SHALL accept `"dialogue"` as a valid value in addition to existing styles. When `style` is `"dialogue"`, the system SHALL use the `DialogueComposer` for script generation and require ElevenLabs as the TTS provider.

#### Scenario: Dialogue style accepted
- **WHEN** a podcast create request includes `style: "dialogue"`
- **THEN** the podcast is created successfully with the dialogue style

#### Scenario: Dialogue style requires ElevenLabs provider
- **WHEN** a podcast create request includes `style: "dialogue"` and `tts_provider: "openai"`
- **THEN** the system returns HTTP 400 indicating dialogue style requires ElevenLabs provider

#### Scenario: Dialogue style requires multiple voices
- **WHEN** a podcast create request includes `style: "dialogue"` and `tts_voices: {"default": "nova"}`
- **THEN** the system returns HTTP 400 indicating dialogue style requires at least two voice roles (e.g., host and cohost)

### Requirement: Customization fields in podcast CRUD
All customization fields SHALL be accepted as optional fields in the podcast create (`POST`) and update (`PUT`) endpoints. The API response for a podcast SHALL include all customization fields with their effective values (stored value or default). The `llmModels` field SHALL be accepted and returned as a JSON object mapping stage names to model names (e.g., `{"filter": "cheap", "compose": "capable"}`). The old `llmModel` field SHALL no longer be accepted. All nullable primitive-typed DTO fields (`Int?`, `Boolean?`, `Double?`) SHALL use Jackson 3 `@JsonProperty` annotations to ensure correct deserialization.

The `maxLlmCostCents` field SHALL be accepted as an optional nullable integer in the podcast create (`POST`) and update (`PUT`) endpoints. When set, it overrides the global `app.llm.max-cost-cents` threshold for that podcast's LLM pipeline cost gate. When null, the global default applies. The field SHALL be included in the podcast GET response.

The `ttsProvider`, `ttsVoices`, and `ttsSettings` fields SHALL be accepted in podcast create and update endpoints and included in GET responses. The `ttsVoices` and `ttsSettings` fields SHALL be returned as JSON objects.

#### Scenario: Create podcast with TTS provider config
- **WHEN** a `POST /users/{userId}/podcasts` request includes `ttsProvider: "elevenlabs"`, `ttsVoices: {"host": "id1", "cohost": "id2"}`, and `ttsSettings: {"stability": 0.5}`
- **THEN** the podcast is created with the specified TTS configuration

#### Scenario: Update podcast TTS provider
- **WHEN** a `PUT /users/{userId}/podcasts/{podcastId}` request includes `ttsProvider: "elevenlabs"`
- **THEN** the podcast's TTS provider is updated to ElevenLabs

#### Scenario: Get podcast includes TTS config
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}` request is received
- **THEN** the response includes `ttsProvider`, `ttsVoices`, and `ttsSettings` fields

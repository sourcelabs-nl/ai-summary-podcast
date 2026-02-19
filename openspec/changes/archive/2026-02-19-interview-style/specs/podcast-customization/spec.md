## MODIFIED Requirements

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
- **WHEN** a podcast create request includes `style: "dialogue"` and `ttsProvider: "openai"`
- **THEN** the system returns HTTP 400 indicating dialogue style requires ElevenLabs provider

#### Scenario: Dialogue style requires multiple voices
- **WHEN** a podcast create request includes `style: "dialogue"` and `ttsVoices: {"default": "nova"}`
- **THEN** the system returns HTTP 400 indicating dialogue style requires at least two voice roles (e.g., host and cohost)

#### Scenario: Podcast with casual style
- **WHEN** a podcast has `style` set to `"casual"`
- **THEN** the briefing composer uses a conversational, relaxed tone in its system prompt

#### Scenario: Podcast with default style
- **WHEN** a podcast is created without specifying `style`
- **THEN** the `style` defaults to `"news-briefing"`

## ADDED Requirements

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

#### Scenario: Speaker names accepted in update endpoint
- **WHEN** a `PUT /users/{userId}/podcasts/{podcastId}` request includes `speakerNames: {"interviewer": "Alice", "expert": "Bob"}`
- **THEN** the podcast's speaker names are updated

#### Scenario: Speaker names included in GET response
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}` request is received for a podcast with `speakerNames`
- **THEN** the response includes the `speakerNames` field

## ADDED Requirements

### Requirement: Inworld AI TTS provider
The system SHALL provide an `InworldTtsProvider` implementing `TtsProvider` that generates audio via the Inworld AI TTS API. The provider SHALL send text to `POST https://api.inworld.ai/tts/v1/voice` with the configured `voiceId` and `modelId`. The response SHALL contain base64-encoded audio in `audioContent` and character usage in `usage.processedCharactersCount`. The provider SHALL decode the base64 audio into byte arrays. The default model SHALL be `inworld-tts-1.5-max`, overridable via `ttsSettings["model"]`. The audio format SHALL be MP3 at 48000 Hz sample rate and 128000 bps bit rate. The provider SHALL use `TextChunker` with its `maxChunkSize` of 2000 to split long scripts. The result SHALL have `requiresConcatenation = true` when multiple chunks are generated.

#### Scenario: Single chunk generates audio
- **WHEN** a script of 1500 characters is sent to the Inworld provider
- **THEN** the provider makes one API call and returns a `TtsResult` with one audio chunk and `requiresConcatenation = false`

#### Scenario: Long script chunked and concatenated
- **WHEN** a script of 5000 characters is sent to the Inworld provider
- **THEN** the provider splits it into 3+ chunks via `TextChunker`, makes one API call per chunk, and returns a `TtsResult` with multiple audio chunks and `requiresConcatenation = true`

#### Scenario: Model override via ttsSettings
- **WHEN** a podcast has `ttsSettings: {"model": "inworld-tts-1.5-mini"}`
- **THEN** the Inworld API is called with `modelId: "inworld-tts-1.5-mini"`

#### Scenario: Default model used when not specified
- **WHEN** a podcast has no `"model"` key in `ttsSettings`
- **THEN** the Inworld API is called with `modelId: "inworld-tts-1.5-max"`

#### Scenario: Voice resolved from ttsVoices map
- **WHEN** a podcast has `ttsVoices: {"default": "some-inworld-voice-id"}`
- **THEN** the Inworld API is called with `voiceId: "some-inworld-voice-id"`

#### Scenario: Character count tracked from API response
- **WHEN** the Inworld API returns `usage.processedCharactersCount: 1800`
- **THEN** the `TtsResult.totalCharacters` reflects the sum of processed characters across all chunks

### Requirement: Inworld API client with JWT authentication
The system SHALL provide an `InworldApiClient` that authenticates to the Inworld AI API using JWT. The client SHALL construct a JWT token signed with the secret from `INWORLD_AI_JWT_SECRET`, using the key ID from `INWORLD_AI_JWT_KEY`. The JWT SHALL be sent in the `Authorization` header as `Bearer <token>`. Per-user credentials SHALL be resolved via `UserProviderConfigService` with `ApiKeyCategory.TTS` and provider name `"inworld"`, falling back to the global env vars.

#### Scenario: Authentication with global env vars
- **WHEN** no per-user Inworld config exists and `INWORLD_AI_JWT_KEY` and `INWORLD_AI_JWT_SECRET` env vars are set
- **THEN** the client uses the global credentials to construct the JWT

#### Scenario: Authentication with per-user config
- **WHEN** a user has configured Inworld TTS provider credentials
- **THEN** the client uses the user's credentials instead of global env vars

#### Scenario: Missing credentials
- **WHEN** neither per-user config nor global env vars provide Inworld credentials
- **THEN** the client throws an `IllegalStateException` with a message indicating Inworld API credentials must be configured

### Requirement: Inworld API error handling
The system SHALL handle Inworld API errors with clear error messages. HTTP 401 SHALL indicate invalid or expired credentials. HTTP 429 SHALL indicate rate limiting. Other error status codes SHALL include the HTTP status and response body in the error message.

#### Scenario: Invalid credentials
- **WHEN** the Inworld API returns HTTP 401
- **THEN** the system throws an error indicating Inworld API credentials are invalid or expired

#### Scenario: Rate limit exceeded
- **WHEN** the Inworld API returns HTTP 429
- **THEN** the system throws an error indicating the Inworld rate limit was exceeded

#### Scenario: Generic API error
- **WHEN** the Inworld API returns HTTP 500
- **THEN** the system logs the error body and throws an error with the HTTP status and body

### Requirement: Inworld TTS script guidelines
The `InworldTtsProvider` SHALL return style-aware script guidelines via `scriptGuidelines(style)`. The guidelines SHALL instruct the LLM to use Inworld-specific markup:
- Non-verbal tags: `[sigh]`, `[laugh]`, `[breathe]`, `[cough]`, `[clear_throat]`, `[yawn]`
- Emphasis: `*word*` (single asterisks) for stressed words
- Pacing: ellipsis (`...`) for trailing pauses, exclamation marks for excitement
- IPA phonemes: `/phoneme/` for precise pronunciation of proper nouns

For `CASUAL` and `DIALOGUE` styles, guidelines SHALL additionally encourage natural filler words (`uh`, `um`, `well`, `you know`). For `EXECUTIVE_SUMMARY` and `NEWS_BRIEFING` styles, guidelines SHALL instruct to avoid filler words and minimize non-verbal tags.

#### Scenario: Casual style guidelines include filler words
- **WHEN** `scriptGuidelines(PodcastStyle.CASUAL)` is called
- **THEN** the returned text includes instructions to use filler words naturally

#### Scenario: Executive summary guidelines suppress filler words
- **WHEN** `scriptGuidelines(PodcastStyle.EXECUTIVE_SUMMARY)` is called
- **THEN** the returned text instructs to avoid filler words and minimize non-verbal tags

#### Scenario: All styles include core markup instructions
- **WHEN** `scriptGuidelines()` is called for any style
- **THEN** the returned text includes instructions for non-verbal tags, emphasis, pacing, and IPA phonemes

### Requirement: Inworld TTS max chunk size
The `InworldTtsProvider` SHALL declare `maxChunkSize = 2000` matching the Inworld API's 2000 character limit per request.

#### Scenario: Max chunk size is 2000
- **WHEN** the Inworld provider's `maxChunkSize` is queried
- **THEN** it returns 2000

### Requirement: Inworld voice discovery
The system SHALL provide voice listing for Inworld AI via the `InworldApiClient`. The client SHALL call the Inworld voices API and return a list of `VoiceInfo` objects with `voiceId`, `name`, `category`, and `previewUrl`.

#### Scenario: List Inworld voices
- **WHEN** `listVoices(userId)` is called on `InworldApiClient`
- **THEN** the client calls the Inworld voices API and returns available voices

#### Scenario: Inworld voices API error
- **WHEN** the Inworld voices API returns an error
- **THEN** the error is handled with the same pattern as other Inworld API errors

### Requirement: Inworld TTS provider type
The `TtsProviderType` enum SHALL include an `INWORLD` entry with value `"inworld"`.

#### Scenario: Inworld provider type serialization
- **WHEN** a podcast has `ttsProvider: TtsProviderType.INWORLD`
- **THEN** it serializes to JSON as `"inworld"`

#### Scenario: Inworld provider type deserialization
- **WHEN** a JSON payload contains `"ttsProvider": "inworld"`
- **THEN** it deserializes to `TtsProviderType.INWORLD`

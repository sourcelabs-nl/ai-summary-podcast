## MODIFIED Requirements

### Requirement: Inworld AI TTS provider
The system SHALL provide an `InworldTtsProvider` implementing `TtsProvider` that generates audio via the Inworld AI TTS API. The provider SHALL send text to `POST https://api.inworld.ai/tts/v1/voice` with the configured `voiceId` and `modelId`. The response SHALL contain base64-encoded audio in `audioContent` and character usage in `usage.processedCharactersCount`. The provider SHALL decode the base64 audio into byte arrays. The default model SHALL be `inworld-tts-1.5-max`, overridable via `ttsSettings["model"]`. The audio format SHALL be MP3 at 48000 Hz sample rate and 128000 bps bit rate. The provider SHALL use `TextChunker` with its `maxChunkSize` of 2000 to split long scripts. The result SHALL have `requiresConcatenation = true` when multiple chunks are generated. The provider SHALL generate all chunks concurrently using Kotlin coroutines (`async`/`awaitAll` on `Dispatchers.IO`). Chunk ordering in the result SHALL match the original script order regardless of completion order.

#### Scenario: Single chunk generates audio
- **WHEN** a script of 1500 characters is sent to the Inworld provider
- **THEN** the provider makes one API call and returns a `TtsResult` with one audio chunk and `requiresConcatenation = false`

#### Scenario: Long script chunked and generated in parallel
- **WHEN** a script of 5000 characters is sent to the Inworld provider
- **THEN** the provider splits it into 3+ chunks via `TextChunker`, generates all chunks concurrently via coroutines, and returns a `TtsResult` with audio chunks in script order and `requiresConcatenation = true`

#### Scenario: Model override via ttsSettings
- **WHEN** a podcast has `ttsSettings: {"model": "inworld-tts-1.5-mini"}`
- **THEN** the Inworld API is called with `modelId: "inworld-tts-1.5-mini"`

#### Scenario: Default model used when not specified
- **WHEN** a podcast has no `"model"` key in `ttsSettings`
- **THEN** the Inworld API is called with `modelId: "inworld-tts-1.5-max"`

#### Scenario: Speed setting applied via ttsSettings
- **WHEN** a podcast has `ttsSettings: {"speed": "1.2"}`
- **THEN** the Inworld API request includes `audioConfig.speakingRate: 1.2` (valid range: 0.5-1.5, default: 1.0)

#### Scenario: Temperature setting applied via ttsSettings
- **WHEN** a podcast has `ttsSettings: {"temperature": "0.8"}`
- **THEN** the Inworld API request includes `temperature: 0.8` as a top-level field (valid range: >0 to 2.0, default: 1.1)

#### Scenario: Speed and temperature omitted when not configured
- **WHEN** a podcast has no `"speed"` or `"temperature"` keys in `ttsSettings`
- **THEN** the Inworld API request does not include `speakingRate` in `audioConfig` or `temperature` at the top level, letting the API use its defaults

#### Scenario: Voice resolved from ttsVoices map
- **WHEN** a podcast has `ttsVoices: {"default": "some-inworld-voice-id"}`
- **THEN** the Inworld API is called with `voiceId: "some-inworld-voice-id"`

#### Scenario: Character count tracked from API response
- **WHEN** the Inworld API returns `usage.processedCharactersCount: 1800`
- **THEN** the `TtsResult.totalCharacters` reflects the sum of processed characters across all chunks

#### Scenario: Dialogue chunks generated in parallel
- **WHEN** a dialogue script with 4 turns (total 6000 chars) is sent to the Inworld provider
- **THEN** the provider flattens all turn chunks into a single list and generates them all concurrently, returning audio chunks in original turn/chunk order

### Requirement: Inworld API error handling
The system SHALL handle Inworld API errors with clear error messages. HTTP 401 SHALL indicate invalid or expired credentials. HTTP 429 SHALL be retried with exponential backoff (up to 3 attempts with delays of 1s, 2s, 4s). If retries are exhausted, the system SHALL throw an `InworldRateLimitException`. Other error status codes SHALL include the HTTP status and response body in the error message.

#### Scenario: Invalid credentials
- **WHEN** the Inworld API returns HTTP 401
- **THEN** the system throws an error indicating Inworld API credentials are invalid or expired

#### Scenario: Rate limit exceeded with successful retry
- **WHEN** the Inworld API returns HTTP 429 on the first attempt
- **AND** the retry succeeds on the second attempt
- **THEN** the system returns the successful response after a 1-second backoff delay

#### Scenario: Rate limit exceeded with exhausted retries
- **WHEN** the Inworld API returns HTTP 429 on all 3 retry attempts
- **THEN** the system throws an `InworldRateLimitException` indicating the rate limit was exceeded after retries

#### Scenario: Generic API error
- **WHEN** the Inworld API returns HTTP 500
- **THEN** the system logs the error body and throws an error with the HTTP status and body (no retry)

#### Scenario: Partial failure in parallel generation
- **WHEN** 6 chunks are generated in parallel and 1 chunk fails after retry exhaustion
- **THEN** the entire generation fails with the error from the failed chunk

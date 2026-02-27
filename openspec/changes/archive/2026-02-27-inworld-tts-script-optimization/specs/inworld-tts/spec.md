## MODIFIED Requirements

### Requirement: Inworld AI TTS provider
The system SHALL provide an `InworldTtsProvider` implementing `TtsProvider` that generates audio via the Inworld AI TTS API. The provider SHALL send text to `POST https://api.inworld.ai/tts/v1/voice` with the configured `voiceId` and `modelId`. The response SHALL contain base64-encoded audio in `audioContent` and character usage in `usage.processedCharactersCount`. The provider SHALL decode the base64 audio into byte arrays. The default model SHALL be `inworld-tts-1.5-max`, overridable via `ttsSettings["model"]`. The audio format SHALL be MP3 at 48000 Hz sample rate and 128000 bps bit rate. The provider SHALL use `TextChunker` with its `maxChunkSize` of 2000 to split long scripts. The result SHALL have `requiresConcatenation = true` when multiple chunks are generated. The provider SHALL generate all chunks concurrently using Kotlin coroutines (`async`/`awaitAll` on `Dispatchers.IO`). Chunk ordering in the result SHALL match the original script order regardless of completion order. When no explicit `temperature` is configured in `ttsSettings`, the provider SHALL default to `0.8` instead of omitting the parameter.

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

#### Scenario: Default temperature of 0.8 when not configured
- **WHEN** a podcast has no `"temperature"` key in `ttsSettings`
- **THEN** the Inworld API request includes `temperature: 0.8`

#### Scenario: Speed and temperature omitted when not configured
- **WHEN** a podcast has no `"speed"` key in `ttsSettings`
- **THEN** the Inworld API request does not include `speakingRate` in `audioConfig`, letting the API use its default

#### Scenario: Voice resolved from ttsVoices map
- **WHEN** a podcast has `ttsVoices: {"default": "some-inworld-voice-id"}`
- **THEN** the Inworld API is called with `voiceId: "some-inworld-voice-id"`

#### Scenario: Character count tracked from API response
- **WHEN** the Inworld API returns `usage.processedCharactersCount: 1800`
- **THEN** the `TtsResult.totalCharacters` reflects the sum of processed characters across all chunks

#### Scenario: Dialogue chunks generated in parallel
- **WHEN** a dialogue script with 4 turns (total 6000 chars) is sent to the Inworld provider
- **THEN** the provider flattens all turn chunks into a single list and generates them all concurrently, returning audio chunks in original turn/chunk order

### Requirement: Inworld API sends applyTextNormalization
The `InworldApiClient.synthesizeSpeech()` SHALL always include `applyTextNormalization: true` in the request body. This enables Inworld's built-in text normalization as a safety net for numbers, dates, and currencies that the LLM may not have expanded to spoken form.

#### Scenario: Text normalization enabled in API request
- **WHEN** a TTS request is sent to the Inworld API
- **THEN** the request body includes `"applyTextNormalization": true`

### Requirement: Inworld TTS script guidelines
The `InworldTtsProvider` SHALL return style-aware script guidelines via `scriptGuidelines(style)`. The guidelines SHALL instruct the LLM to use Inworld-specific markup:
- Non-verbal tags: `[sigh]`, `[laugh]`, `[breathe]`, `[cough]`, `[clear_throat]`, `[yawn]`
- Emphasis: `*word*` (single asterisks) for stressed words
- Pacing: ellipsis (`...`) for trailing pauses, exclamation marks for excitement
- IPA phonemes: `/phoneme/` for precise pronunciation of proper nouns

The guidelines SHALL additionally include:
- Text normalization: write all numbers, dates, currencies, and symbols in fully spoken form
- Anti-markdown: never use markdown formatting; never use `**double asterisks**` as the TTS engine reads asterisk characters aloud
- Contractions: use natural contractions throughout for spoken naturalness
- Punctuation: always end sentences with proper punctuation for correct pacing

For `CASUAL` and `DIALOGUE` styles, guidelines SHALL additionally encourage natural filler words (`uh`, `um`, `well`, `you know`). For `EXECUTIVE_SUMMARY` and `NEWS_BRIEFING` styles, guidelines SHALL instruct to avoid filler words and minimize non-verbal tags.

#### Scenario: Casual style guidelines include filler words
- **WHEN** `scriptGuidelines(PodcastStyle.CASUAL)` is called
- **THEN** the returned text includes instructions to use filler words naturally

#### Scenario: Executive summary guidelines suppress filler words
- **WHEN** `scriptGuidelines(PodcastStyle.EXECUTIVE_SUMMARY)` is called
- **THEN** the returned text instructs to avoid filler words and minimize non-verbal tags

#### Scenario: All styles include core markup and formatting instructions
- **WHEN** `scriptGuidelines()` is called for any style
- **THEN** the returned text includes instructions for non-verbal tags, emphasis, pacing, IPA phonemes, text normalization, anti-markdown, contractions, and punctuation

#### Scenario: Guidelines warn against double asterisks
- **WHEN** `scriptGuidelines()` is called for any style
- **THEN** the returned text explicitly warns that `**double asterisks**` will cause the TTS engine to read asterisk characters aloud

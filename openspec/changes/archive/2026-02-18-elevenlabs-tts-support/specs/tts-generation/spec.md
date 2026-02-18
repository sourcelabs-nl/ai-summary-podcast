## MODIFIED Requirements

### Requirement: TTS audio generation via OpenAI API
The system SHALL refactor the existing OpenAI TTS logic into an `OpenAiTtsProvider` implementing the `TtsProvider` interface. The provider SHALL send each text chunk to the OpenAI TTS API using a manually configured `OpenAiAudioSpeechModel` bean. The voice SHALL be resolved from the `"default"` key in `ttsVoices`. The speed SHALL be resolved from the `"speed"` key in `ttsSettings` (defaulting to `1.0`). Each chunk SHALL produce an MP3 audio segment. The TTS API auto-detects the language from the input text; no explicit language parameter is sent. Logging SHALL include the podcast's configured language for observability. The provider SHALL track the total character count of all chunks sent to the API and return it alongside the audio data. The result SHALL have `requiresConcatenation = true` when multiple chunks are generated.

#### Scenario: Chunk converted to audio
- **WHEN** a text chunk is sent to the TTS API
- **THEN** an MP3 audio byte array is returned for that chunk

#### Scenario: TTS API error handling
- **WHEN** the TTS API returns an error for a chunk
- **THEN** the error is logged and the entire episode generation fails with a clear error message

#### Scenario: Non-English text produces non-English audio
- **WHEN** a text chunk in Dutch is sent to the TTS API
- **THEN** the TTS API auto-detects Dutch and produces Dutch-language audio

#### Scenario: TTS logging includes language
- **WHEN** TTS audio generation starts for a podcast with language `"nl"`
- **THEN** the log message includes the language, e.g. "Generating TTS audio for 3 chunks (voice: nova, speed: 1.0, language: nl)"

#### Scenario: Character count tracked across all chunks
- **WHEN** 3 text chunks of 2000, 3000, and 1500 characters are sent to the TTS API
- **THEN** the returned total character count is 6500

#### Scenario: Character count included in TTS result
- **WHEN** the TTS service generates audio for an episode
- **THEN** the result includes both the audio byte arrays and the total character count

#### Scenario: Voice resolved from ttsVoices map
- **WHEN** a podcast has `ttsVoices: {"default": "alloy"}`
- **THEN** the OpenAI TTS API is called with voice `"alloy"`

#### Scenario: Speed resolved from ttsSettings map
- **WHEN** a podcast has `ttsSettings: {"speed": 1.25}`
- **THEN** the OpenAI TTS API is called with speed `1.25`

#### Scenario: Default speed when not configured
- **WHEN** a podcast has `ttsSettings` without a `"speed"` key
- **THEN** the OpenAI TTS API is called with speed `1.0`

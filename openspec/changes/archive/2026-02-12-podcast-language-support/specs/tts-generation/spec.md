## MODIFIED Requirements

### Requirement: TTS audio generation via OpenAI API
The system SHALL send each text chunk to the OpenAI TTS API using a manually configured `OpenAiAudioSpeechModel` bean (separate from the OpenRouter ChatClient). The voice and model (e.g., `alloy`, `tts-1-hd`) SHALL be configurable via application properties. Each chunk SHALL produce an MP3 audio segment. The TTS API auto-detects the language from the input text; no explicit language parameter is sent. Logging SHALL include the podcast's configured language for observability.

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

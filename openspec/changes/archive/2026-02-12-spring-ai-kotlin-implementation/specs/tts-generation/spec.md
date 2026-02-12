## ADDED Requirements

### Requirement: Text chunking at sentence boundaries
The system SHALL split the briefing script into chunks that respect the TTS API's 4096 character limit. Chunks SHALL be split at sentence boundaries (period, exclamation mark, question mark followed by whitespace) to avoid mid-sentence audio cuts.

#### Scenario: Long script split into multiple chunks
- **WHEN** a briefing script of 8000 characters is processed
- **THEN** the script is split into 2 or more chunks, each at most 4096 characters, with splits occurring at sentence boundaries

#### Scenario: Short script kept as single chunk
- **WHEN** a briefing script of 3000 characters is processed
- **THEN** the script is sent as a single chunk without splitting

#### Scenario: Very long sentence handling
- **WHEN** a single sentence exceeds 4096 characters
- **THEN** the sentence is split at the nearest whitespace before the 4096 limit

### Requirement: TTS audio generation via OpenAI API
The system SHALL send each text chunk to the OpenAI TTS API using a manually configured `OpenAiAudioSpeechModel` bean (separate from the OpenRouter ChatClient). The voice and model (e.g., `alloy`, `tts-1-hd`) SHALL be configurable via application properties. Each chunk SHALL produce an MP3 audio segment.

#### Scenario: Chunk converted to audio
- **WHEN** a text chunk is sent to the TTS API
- **THEN** an MP3 audio byte array is returned for that chunk

#### Scenario: TTS API error handling
- **WHEN** the TTS API returns an error for a chunk
- **THEN** the error is logged and the entire episode generation fails with a clear error message

### Requirement: Audio concatenation via FFmpeg
The system SHALL concatenate all audio chunks into a single MP3 file using FFmpeg via `ProcessBuilder`. The output file SHALL be stored in the configured episodes directory.

#### Scenario: Multiple chunks concatenated
- **WHEN** 3 audio chunks are generated from TTS
- **THEN** FFmpeg concatenates them into a single MP3 file in the episodes directory

#### Scenario: Single chunk skips concatenation
- **WHEN** only 1 audio chunk is generated
- **THEN** the chunk is written directly as the final MP3 file without invoking FFmpeg

#### Scenario: FFmpeg not available
- **WHEN** FFmpeg is not installed on the system
- **THEN** the application logs a clear error at startup or at concatenation time

### Requirement: Audio duration calculation
The system SHALL calculate the duration in seconds of the final MP3 file for use in episode metadata and the podcast RSS feed.

#### Scenario: Duration calculated after concatenation
- **WHEN** a final MP3 file is produced
- **THEN** the duration in seconds is calculated and stored with the episode record

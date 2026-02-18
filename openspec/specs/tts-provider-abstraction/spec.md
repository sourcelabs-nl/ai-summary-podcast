## ADDED Requirements

### Requirement: TTS provider interface
The system SHALL define a `TtsProvider` interface with a `generate(request: TtsRequest): TtsResult` method. The `TtsRequest` SHALL contain the script text, voice configuration (`ttsVoices` map), provider-specific settings (`ttsSettings` map), and language. The `TtsResult` SHALL contain audio chunks (list of byte arrays), total character count, and a flag indicating whether FFmpeg concatenation is needed.

#### Scenario: Provider generates audio from request
- **WHEN** a `TtsProvider.generate()` is called with a valid `TtsRequest`
- **THEN** the provider returns a `TtsResult` with audio data and character count

#### Scenario: Provider signals concatenation not needed
- **WHEN** a provider returns a single audio file (e.g., ElevenLabs dialogue)
- **THEN** `TtsResult.requiresConcatenation` is `false` and `audioChunks` contains exactly one element

#### Scenario: Provider signals concatenation needed
- **WHEN** a provider returns multiple audio chunks (e.g., OpenAI TTS with a long script)
- **THEN** `TtsResult.requiresConcatenation` is `true` and `audioChunks` contains multiple elements

### Requirement: TTS provider factory
The system SHALL provide a `TtsProviderFactory` that resolves the appropriate `TtsProvider` implementation based on `podcast.ttsProvider` and `podcast.style`. The factory SHALL be a Spring component that receives all provider implementations via constructor injection.

#### Scenario: OpenAI provider resolved
- **WHEN** a podcast has `ttsProvider` set to `"openai"`
- **THEN** the factory returns the `OpenAiTtsProvider`

#### Scenario: ElevenLabs monologue provider resolved
- **WHEN** a podcast has `ttsProvider` set to `"elevenlabs"` and `style` is not `"dialogue"`
- **THEN** the factory returns the `ElevenLabsTtsProvider`

#### Scenario: ElevenLabs dialogue provider resolved
- **WHEN** a podcast has `ttsProvider` set to `"elevenlabs"` and `style` is `"dialogue"`
- **THEN** the factory returns the `ElevenLabsDialogueTtsProvider`

#### Scenario: Unknown provider rejected
- **WHEN** a podcast has `ttsProvider` set to an unsupported value
- **THEN** the factory throws an `IllegalArgumentException` with a message listing supported providers

### Requirement: TtsPipeline uses provider abstraction
The `TtsPipeline` SHALL resolve the `TtsProvider` via the `TtsProviderFactory` based on the podcast's configuration. It SHALL construct a `TtsRequest` from the script and podcast settings, call the provider, and conditionally invoke FFmpeg concatenation based on `TtsResult.requiresConcatenation`.

#### Scenario: Pipeline delegates to resolved provider
- **WHEN** the pipeline generates audio for a podcast with `ttsProvider` set to `"elevenlabs"`
- **THEN** the pipeline calls the ElevenLabs provider, not the OpenAI provider

#### Scenario: Pipeline skips concatenation when not required
- **WHEN** the provider returns `requiresConcatenation = false`
- **THEN** the pipeline writes the single audio chunk directly to disk without invoking FFmpeg

#### Scenario: Pipeline concatenates when required
- **WHEN** the provider returns `requiresConcatenation = true` with multiple chunks
- **THEN** the pipeline invokes FFmpeg to concatenate the chunks into a single MP3

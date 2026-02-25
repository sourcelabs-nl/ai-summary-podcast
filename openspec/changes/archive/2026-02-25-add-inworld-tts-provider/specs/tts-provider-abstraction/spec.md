## MODIFIED Requirements

### Requirement: TTS provider interface
The system SHALL define a `TtsProvider` interface with a `generate(request: TtsRequest): TtsResult` method, a `scriptGuidelines(style: PodcastStyle): String` method, and a `maxChunkSize: Int` property. The `TtsRequest` SHALL contain the script text, voice configuration (`ttsVoices` map), provider-specific settings (`ttsSettings` map), and language. The `TtsResult` SHALL contain audio chunks (list of byte arrays), total character count, and a flag indicating whether FFmpeg concatenation is needed.

#### Scenario: Provider generates audio from request
- **WHEN** a `TtsProvider.generate()` is called with a valid `TtsRequest`
- **THEN** the provider returns a `TtsResult` with audio data and character count

#### Scenario: Provider signals concatenation not needed
- **WHEN** a provider returns a single audio file (e.g., ElevenLabs dialogue)
- **THEN** `TtsResult.requiresConcatenation` is `false` and `audioChunks` contains exactly one element

#### Scenario: Provider signals concatenation needed
- **WHEN** a provider returns multiple audio chunks (e.g., OpenAI TTS with a long script)
- **THEN** `TtsResult.requiresConcatenation` is `true` and `audioChunks` contains multiple elements

#### Scenario: Provider declares script guidelines
- **WHEN** `scriptGuidelines(style)` is called on any `TtsProvider`
- **THEN** the provider returns a string with LLM prompt instructions for formatting scripts

#### Scenario: Provider declares max chunk size
- **WHEN** `maxChunkSize` is accessed on any `TtsProvider`
- **THEN** the provider returns a positive integer representing the maximum characters per API request

### Requirement: TTS provider factory
The system SHALL provide a `TtsProviderFactory` that resolves the appropriate `TtsProvider` implementation based on `podcast.ttsProvider` and `podcast.style`. The factory SHALL be a Spring component that receives all provider implementations via constructor injection.

#### Scenario: OpenAI provider resolved
- **WHEN** a podcast has `ttsProvider` set to `"openai"`
- **THEN** the factory returns the `OpenAiTtsProvider`

#### Scenario: ElevenLabs monologue provider resolved
- **WHEN** a podcast has `ttsProvider` set to `"elevenlabs"` and `style` is not `"dialogue"` or `"interview"`
- **THEN** the factory returns the `ElevenLabsTtsProvider`

#### Scenario: ElevenLabs dialogue provider resolved
- **WHEN** a podcast has `ttsProvider` set to `"elevenlabs"` and `style` is `"dialogue"` or `"interview"`
- **THEN** the factory returns the `ElevenLabsDialogueTtsProvider`

#### Scenario: Inworld monologue provider resolved
- **WHEN** a podcast has `ttsProvider` set to `"inworld"` and `style` is not `"dialogue"` or `"interview"`
- **THEN** the factory returns the `InworldTtsProvider`

#### Scenario: Inworld dialogue provider resolved
- **WHEN** a podcast has `ttsProvider` set to `"inworld"` and `style` is `"dialogue"` or `"interview"`
- **THEN** the factory returns the `InworldTtsProvider` (same provider, handles per-turn generation internally by parsing dialogue turns and generating each with the appropriate voice)

#### Scenario: Unknown provider rejected
- **WHEN** a podcast has `ttsProvider` set to an unsupported value
- **THEN** the factory throws an `IllegalArgumentException` with a message listing supported providers

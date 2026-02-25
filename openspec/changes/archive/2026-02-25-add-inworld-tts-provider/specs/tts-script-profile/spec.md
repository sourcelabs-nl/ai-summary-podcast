## ADDED Requirements

### Requirement: TTS provider declares script guidelines
Each `TtsProvider` implementation SHALL provide a `scriptGuidelines(style: PodcastStyle): String` method that returns LLM prompt instructions specific to that provider's capabilities. The guidelines describe what markup, emotion tags, or formatting the TTS engine supports so the LLM can generate optimized scripts.

#### Scenario: OpenAI returns plain text guidelines
- **WHEN** `scriptGuidelines()` is called on `OpenAiTtsProvider`
- **THEN** it returns instructions to write clean natural speech without special markup, emotion tags, or non-verbal cues

#### Scenario: ElevenLabs returns emotion cue guidelines
- **WHEN** `scriptGuidelines()` is called on `ElevenLabsTtsProvider`
- **THEN** it returns instructions allowing emotion cues in square brackets (e.g., `[cheerfully]`, `[seriously]`)

#### Scenario: ElevenLabs dialogue provider shares single-speaker guidelines
- **WHEN** `scriptGuidelines()` is called on `ElevenLabsDialogueTtsProvider`
- **THEN** it returns the same emotion cue guidelines as `ElevenLabsTtsProvider`

### Requirement: TTS provider declares max chunk size
Each `TtsProvider` implementation SHALL declare a `maxChunkSize: Int` property indicating the maximum number of characters per TTS API request.

#### Scenario: OpenAI max chunk size
- **WHEN** `OpenAiTtsProvider.maxChunkSize` is queried
- **THEN** it returns 4096

#### Scenario: ElevenLabs max chunk size
- **WHEN** `ElevenLabsTtsProvider.maxChunkSize` is queried
- **THEN** it returns 5000

#### Scenario: ElevenLabs dialogue max chunk size
- **WHEN** `ElevenLabsDialogueTtsProvider.maxChunkSize` is queried
- **THEN** it returns 5000

### Requirement: Composers inject TTS script guidelines into prompts
All three composers (`BriefingComposer`, `DialogueComposer`, `InterviewComposer`) SHALL accept a `ttsScriptGuidelines: String` parameter in their `compose()` methods. The guidelines SHALL be injected into the LLM prompt as an additional block after the existing requirements section. When the guidelines string is empty, no additional block SHALL be added.

#### Scenario: Briefing composer includes Inworld guidelines
- **WHEN** `BriefingComposer.compose()` is called with Inworld script guidelines
- **THEN** the LLM prompt includes the Inworld emotion tags and emphasis instructions

#### Scenario: Dialogue composer includes provider guidelines
- **WHEN** `DialogueComposer.compose()` is called with ElevenLabs script guidelines
- **THEN** the LLM prompt includes the ElevenLabs emotion cue instructions

#### Scenario: Empty guidelines add nothing to prompt
- **WHEN** a composer is called with an empty string for `ttsScriptGuidelines`
- **THEN** the LLM prompt has no additional TTS-specific block

### Requirement: LlmPipeline resolves and passes TTS script guidelines
The `LlmPipeline` SHALL resolve the `TtsProvider` via `TtsProviderFactory` before calling a composer. It SHALL call `scriptGuidelines(podcast.style)` on the resolved provider and pass the result to the composer. The `LlmPipeline` SHALL gain a constructor dependency on `TtsProviderFactory`.

#### Scenario: Pipeline passes guidelines to briefing composer
- **WHEN** the pipeline runs for a podcast with `ttsProvider: INWORLD` and `style: CASUAL`
- **THEN** it resolves the Inworld provider, calls `scriptGuidelines(CASUAL)`, and passes the result to `BriefingComposer.compose()`

#### Scenario: Pipeline passes guidelines to dialogue composer
- **WHEN** the pipeline runs for a podcast with `ttsProvider: ELEVENLABS` and `style: DIALOGUE`
- **THEN** it resolves the ElevenLabs dialogue provider, calls `scriptGuidelines(DIALOGUE)`, and passes the result to `DialogueComposer.compose()`

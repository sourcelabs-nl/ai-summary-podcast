## MODIFIED Requirements

### Requirement: TTS provider declares script guidelines
Each `TtsProvider` implementation SHALL provide a `scriptGuidelines(style: PodcastStyle): String` method that returns LLM prompt instructions specific to that provider's capabilities. The guidelines describe what markup, emotion tags, or formatting the TTS engine supports so the LLM can generate optimized scripts. For the Inworld provider, guidelines SHALL additionally include text normalization rules, anti-markdown warnings, contractions guidance, and punctuation rules per Inworld's best practices documentation.

#### Scenario: OpenAI returns plain text guidelines
- **WHEN** `scriptGuidelines()` is called on `OpenAiTtsProvider`
- **THEN** it returns instructions to write clean natural speech without special markup, emotion tags, or non-verbal cues

#### Scenario: ElevenLabs returns emotion cue guidelines
- **WHEN** `scriptGuidelines()` is called on `ElevenLabsTtsProvider`
- **THEN** it returns instructions allowing emotion cues in square brackets (e.g., `[cheerfully]`, `[seriously]`)

#### Scenario: ElevenLabs dialogue provider shares single-speaker guidelines
- **WHEN** `scriptGuidelines()` is called on `ElevenLabsDialogueTtsProvider`
- **THEN** it returns the same emotion cue guidelines as `ElevenLabsTtsProvider`

#### Scenario: Inworld returns comprehensive formatting guidelines
- **WHEN** `scriptGuidelines()` is called on `InworldTtsProvider`
- **THEN** it returns instructions covering non-verbal tags, emphasis, pacing, IPA phonemes, text normalization, anti-markdown rules, contractions, and punctuation

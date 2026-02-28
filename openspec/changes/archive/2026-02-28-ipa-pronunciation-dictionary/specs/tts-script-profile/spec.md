## MODIFIED Requirements

### Requirement: TTS provider declares script guidelines
Each `TtsProvider` implementation SHALL provide a `scriptGuidelines(style: PodcastStyle, pronunciations: Map<String, String>): String` method that returns LLM prompt instructions specific to that provider's capabilities. The `pronunciations` parameter SHALL default to an empty map so existing callers without pronunciations continue to work. The guidelines describe what markup, emotion tags, or formatting the TTS engine supports so the LLM can generate optimized scripts. For the Inworld provider, guidelines SHALL additionally include text normalization rules, anti-markdown warnings, contractions guidance, punctuation rules, and a pronunciation guide section when pronunciations are provided.

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

#### Scenario: Inworld includes pronunciation guide when pronunciations provided
- **WHEN** `scriptGuidelines(style, mapOf("Anthropic" to "/ænˈθɹɒpɪk/"))` is called on `InworldTtsProvider`
- **THEN** it returns guidelines that include a "Pronunciation Guide" section with the provided entries

### Requirement: LlmPipeline resolves and passes TTS script guidelines
The `LlmPipeline` SHALL resolve the `TtsProvider` via `TtsProviderFactory` before calling a composer. It SHALL call `scriptGuidelines(podcast.style, podcast.pronunciations ?: emptyMap())` on the resolved provider and pass the result to the composer. The `LlmPipeline` SHALL gain a constructor dependency on `TtsProviderFactory`.

#### Scenario: Pipeline passes guidelines with pronunciations to briefing composer
- **WHEN** the pipeline runs for a podcast with `ttsProvider: INWORLD`, `style: CASUAL`, and `pronunciations: {"Anthropic": "/ænˈθɹɒpɪk/"}`
- **THEN** it resolves the Inworld provider, calls `scriptGuidelines(CASUAL, {"Anthropic": "/ænˈθɹɒpɪk/"})`, and passes the result to `BriefingComposer.compose()`

#### Scenario: Pipeline passes empty pronunciations when none configured
- **WHEN** the pipeline runs for a podcast with `ttsProvider: INWORLD`, `style: CASUAL`, and `pronunciations: null`
- **THEN** it calls `scriptGuidelines(CASUAL, emptyMap())` and passes the result to the composer

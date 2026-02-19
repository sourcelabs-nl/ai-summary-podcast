# Capability: Episode Continuity

## Purpose

Episode continuity context enables podcast scripts to reference previous episodes, creating a sense of ongoing narrative. A recap is generated from each episode's script and stored for use by subsequent episodes.

## Requirements

### Requirement: Episode recap generation
The system SHALL provide an `EpisodeRecapGenerator` component that accepts an episode's `scriptText` and produces a 2-3 sentence plain-text recap of the key topics discussed. The recap SHALL be generated using the filter model (resolved via `ModelResolver` for the `filter` stage). The recap prompt SHALL instruct the LLM to summarize the main topics covered in the episode script concisely, without any meta-commentary or preamble. Token usage from the recap call SHALL be tracked.

#### Scenario: Recap generated from episode script
- **WHEN** `EpisodeRecapGenerator.generate()` is called with a 1500-word episode script
- **THEN** the result is a 2-3 sentence plain-text summary of the key topics discussed

#### Scenario: Recap uses filter model
- **WHEN** the recap is generated
- **THEN** the system uses the model resolved for the `filter` stage via `ModelResolver`

#### Scenario: Token usage tracked
- **WHEN** the recap LLM call uses 800 input tokens and 60 output tokens
- **THEN** those tokens are returned in the `RecapResult` and added to the episode's token totals

### Requirement: Recap stored on episode
The system SHALL store the generated recap as a nullable `recap` column on the `episodes` table. The recap SHALL be generated in `BriefingGenerationScheduler` after saving a new episode and persisted via an update to the episode. If recap generation fails, the episode SHALL remain valid with a null recap. The recap generation token usage SHALL be added to the episode's `llm_input_tokens` and `llm_output_tokens`.

#### Scenario: Recap persisted after episode creation
- **WHEN** a new episode is created and saved
- **THEN** the `EpisodeRecapGenerator` generates a recap from the episode's script and the episode is updated with the recap text

#### Scenario: Recap generation failure does not block episode
- **WHEN** the recap LLM call fails with an exception
- **THEN** the episode remains saved with a null `recap` and the failure is logged

#### Scenario: Old episodes without recap
- **WHEN** the pipeline reads the most recent episode and it was created before the recap feature
- **THEN** the episode's `recap` field is null and the composer receives no continuity context

### Requirement: Continuity context in composition prompts
When a previous episode recap is available, both `BriefingComposer` and `DialogueComposer` SHALL include it in the composition prompt. The prompt SHALL contain a "Previous episode context" section with the recap text. The prompt SHALL instruct the LLM: when today's topics relate to the previous episode, weave in specific references (e.g., "as we discussed last time...," "following up on what we covered previously..."). When today's topics are unrelated to the previous episode, include a brief one-liner referencing the previous episode in the introduction (e.g., "last episode we covered X and Y, today we're looking at..."). The recap parameter SHALL be optional — when null, the prompt SHALL NOT include any previous episode section or continuity instructions.

#### Scenario: Composer receives recap with overlapping topics
- **WHEN** the previous episode recap mentions "AI regulation in the EU" and today's articles include new EU AI Act developments
- **THEN** the composed script references the previous episode specifically (e.g., "as we discussed last time, the EU has been working on AI regulation — today there are new developments...")

#### Scenario: Composer receives recap with unrelated topics
- **WHEN** the previous episode recap mentions "cryptocurrency market trends" and today's articles are about space exploration
- **THEN** the composed script includes a brief one-liner in the intro about the previous episode before moving on to today's topics

#### Scenario: No previous episode exists
- **WHEN** the composer is called with a null recap
- **THEN** the composition prompt does not include any previous episode section and the script has no back-references

#### Scenario: Continuity in dialogue style
- **WHEN** the `DialogueComposer` receives a recap
- **THEN** the speakers naturally reference the previous episode in conversation (e.g., host says "last time we talked about...")

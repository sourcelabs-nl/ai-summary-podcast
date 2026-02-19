## ADDED Requirements

### Requirement: InterviewComposer generates asymmetric speaker-tagged scripts
The system SHALL provide an `InterviewComposer` component that generates interview-style dialogue scripts with two fixed roles: `interviewer` and `expert`. The interviewer SHALL act as an audience surrogate — asking questions, bridging topics, and providing brief reactions (~20% of total words). The expert SHALL deliver the news content, context, and analysis (~80% of total words). The output SHALL use XML-style speaker tags `<interviewer>` and `<expert>`. The composer SHALL use the `compose` model (resolved via `ModelResolver`).

#### Scenario: Interview script generated with two speakers
- **WHEN** the `InterviewComposer` composes a script for a podcast with `ttsVoices: {"interviewer": "id1", "expert": "id2"}`
- **THEN** the output contains alternating `<interviewer>` and `<expert>` tags with the interviewer asking questions and the expert delivering content

#### Scenario: Interviewer turns are short, expert turns are long
- **WHEN** the `InterviewComposer` generates a script
- **THEN** interviewer turns are brief (questions, reactions, transitions) and expert turns contain the substantive news content and analysis

#### Scenario: Composer uses compose model
- **WHEN** the `InterviewComposer` is invoked
- **THEN** it resolves and uses the `compose` stage model via `ModelResolver`

#### Scenario: Tags are not stripped from output
- **WHEN** the LLM produces an interview script with `<interviewer>` and `<expert>` tags
- **THEN** the tags are preserved in the returned script

### Requirement: InterviewComposer prompt engineering
The `InterviewComposer` prompt SHALL instruct the LLM to produce a natural interview-style conversation. The prompt SHALL include: the podcast name, topic, current date, article summaries with source attribution, target word count, and language. The prompt SHALL define the interviewer role as asking questions, bridging between topics, and reacting briefly. The prompt SHALL define the expert role as delivering news content, providing context, and offering analysis. The prompt SHALL specify that ALL text MUST be inside `<interviewer>` or `<expert>` tags. The prompt SHALL allow ElevenLabs emotion cues in square brackets (e.g., `[curious]`, `[thoughtful]`). The prompt SHALL prohibit any text outside of speaker tags, stage directions, sound effects, or meta-commentary.

When `speakerNames` is provided on the podcast, the prompt SHALL instruct the LLM to use these names in conversation (e.g., "So Bob, what happened this week?"). When `speakerNames` is not provided, the prompt SHALL instruct speakers to address each other without names.

When a previous episode recap is provided, the prompt SHALL include a "Previous episode context" section. The prompt SHALL instruct the interviewer to naturally reference the previous episode when bridging topics. When topics are unrelated, the interviewer SHALL briefly mention the previous episode in the opening.

When `customInstructions` is provided on the podcast, the prompt SHALL append them.

#### Scenario: Prompt uses speaker display names
- **WHEN** the podcast has `speakerNames: {"interviewer": "Alice", "expert": "Bob"}`
- **THEN** the prompt instructs the LLM to use "Alice" and "Bob" in conversation while keeping `<interviewer>` and `<expert>` as the XML tags

#### Scenario: Prompt works without speaker names
- **WHEN** the podcast has no `speakerNames`
- **THEN** the prompt instructs speakers to address each other without using names

#### Scenario: Prompt includes article summaries
- **WHEN** 5 articles are passed to the composer
- **THEN** the prompt includes all 5 article summaries with source attribution

#### Scenario: Prompt respects podcast language
- **WHEN** the podcast has `language: "nl"`
- **THEN** the prompt instructs the LLM to write the interview in Dutch

#### Scenario: Prompt allows emotion cues
- **WHEN** the LLM generates an interview script
- **THEN** the output may include cues like `<interviewer>[curious] Wait, what does that mean for...?</interviewer>`

#### Scenario: Custom instructions included
- **WHEN** the podcast has `customInstructions: "Focus on practical implications"`
- **THEN** the prompt includes these instructions

#### Scenario: Interview includes continuity with related topics
- **WHEN** the previous episode recap mentions "AI regulation" and today's articles include new EU AI Act developments
- **THEN** the interviewer naturally references the previous episode (e.g., "We talked about AI regulation last time — any updates?")

#### Scenario: Interview includes brief continuity with unrelated topics
- **WHEN** the previous episode recap mentions "cryptocurrency" and today's articles are about climate policy
- **THEN** the interviewer briefly mentions the previous episode in the opening before moving to today's topics

### Requirement: Interview style routing in pipeline
The LLM pipeline SHALL route `style: "interview"` to the `InterviewComposer`. The selection SHALL happen in the pipeline orchestration layer (`LlmPipeline`).

#### Scenario: Interview style uses InterviewComposer
- **WHEN** a podcast has `style: "interview"`
- **THEN** the pipeline uses `InterviewComposer` for script generation

#### Scenario: Dialogue style still uses DialogueComposer
- **WHEN** a podcast has `style: "dialogue"`
- **THEN** the pipeline uses `DialogueComposer` for script generation

#### Scenario: Monologue styles still use BriefingComposer
- **WHEN** a podcast has `style: "news-briefing"`
- **THEN** the pipeline uses `BriefingComposer` for script generation

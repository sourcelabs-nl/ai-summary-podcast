## MODIFIED Requirements

### Requirement: DialogueComposer prompt engineering
The `DialogueComposer` prompt SHALL instruct the LLM to produce a natural conversation between the configured speaker roles. The prompt SHALL include: the podcast name, topic, current date, article summaries (same format as `BriefingComposer`), target word count, and language. The prompt SHALL specify that the output must use the exact XML tag names corresponding to the `ttsVoices` keys. The prompt SHALL instruct speakers to have distinct personalities — the host drives the conversation and the co-host provides reactions, analysis, and follow-up questions. The prompt SHALL allow the use of ElevenLabs emotion cues in square brackets (e.g., `[cheerfully]`, `[thoughtful pause]`). The prompt SHALL prohibit any text outside of speaker tags.

When a previous episode recap is provided, the prompt SHALL include a "Previous episode context" section containing the recap text. The prompt SHALL instruct the speakers to naturally reference the previous episode in conversation. When today's topics relate to the previous episode, the speakers SHALL weave in specific back-references. When topics are unrelated, the host SHALL include a brief mention of the previous episode in the opening.

#### Scenario: Prompt includes speaker role names
- **WHEN** the podcast has `ttsVoices: {"host": "id1", "cohost": "id2"}`
- **THEN** the prompt instructs the LLM to use `<host>` and `<cohost>` tags

#### Scenario: Prompt includes article summaries
- **WHEN** 5 articles are passed to the composer
- **THEN** the prompt includes all 5 article summaries with source attribution

#### Scenario: Prompt respects podcast language
- **WHEN** the podcast has `language: "nl"`
- **THEN** the prompt instructs the LLM to write the dialogue in Dutch

#### Scenario: Prompt allows emotion cues
- **WHEN** the LLM generates dialogue
- **THEN** the output may include cues like `<host>[excited] Did you see this?</host>`

#### Scenario: Custom instructions included
- **WHEN** the podcast has `customInstructions: "Focus on practical implications"`
- **THEN** the prompt includes these instructions

#### Scenario: Dialogue includes continuity with overlapping topics
- **WHEN** the previous episode recap mentions "AI chip shortage" and today's articles include new developments on chip supply
- **THEN** the dialogue naturally references the previous episode (e.g., host says "remember last time we talked about the chip shortage?")

#### Scenario: Dialogue includes brief continuity with unrelated topics
- **WHEN** the previous episode recap mentions "cryptocurrency trends" and today's articles are about climate policy
- **THEN** the host briefly mentions the previous episode in the opening before moving to today's topics

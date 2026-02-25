## MODIFIED Requirements

### Requirement: InterviewComposer prompt engineering
The `InterviewComposer` prompt SHALL instruct the LLM to produce a natural interview-style conversation. The prompt SHALL include: the podcast name, topic, current date, article summaries with source attribution, target word count, and language. The prompt SHALL define the interviewer role as asking questions, bridging between topics, and reacting briefly. The prompt SHALL define the expert role as delivering news content, providing context, and offering analysis. The prompt SHALL specify that ALL text MUST be inside `<interviewer>` or `<expert>` tags. The prompt SHALL allow ElevenLabs emotion cues in square brackets (e.g., `[curious]`, `[thoughtful]`). The prompt SHALL prohibit any text outside of speaker tags, stage directions, sound effects, or meta-commentary.

The prompt SHALL include transition guidance to ensure natural speaker handovers:
- The prompt SHALL instruct speakers to NOT start turns with bare name addresses (e.g., "Jarno, the market...")
- The prompt SHALL instruct the interviewer to use conversational bridges — reactions, follow-ups, or connectors — before transitioning to a new question or topic (e.g., "That's a great point. Now I'm curious about...", "Interesting. Speaking of which...")
- The prompt SHALL instruct that when names are used, they appear mid-sentence or at the end of questions (e.g., "What do you make of this, Jarno?") rather than as bare turn openers
- The prompt SHALL instruct speakers to vary their transition patterns — not every handover needs to use the other speaker's name

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

#### Scenario: Transitions use conversational bridges not bare name addresses
- **WHEN** the LLM generates an interview script with speaker names configured
- **THEN** speaker turns do not begin with bare name addresses (e.g., NOT "Jarno, the economy...") but instead use natural bridges with names placed mid-sentence or at the end (e.g., "What do you think, Jarno?")

#### Scenario: Transition patterns are varied
- **WHEN** the LLM generates an interview script with 10+ speaker transitions
- **THEN** the transitions use varied patterns — some with reactions, some with follow-up questions, some with topic bridges — rather than a single repeated pattern

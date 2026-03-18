## MODIFIED Requirements

### Requirement: InterviewComposer prompt engineering
The `InterviewComposer` prompt SHALL instruct the LLM to produce a natural interview-style conversation. The prompt SHALL include: the podcast name, topic, current date, article summaries with source attribution, target word count, and language. The prompt SHALL define the interviewer role as asking questions, bridging between topics, and reacting briefly. The prompt SHALL define the expert role as delivering news content, providing context, and offering analysis. The prompt SHALL specify that ALL text MUST be inside `<interviewer>` or `<expert>` tags. The prompt SHALL prohibit any text outside of speaker tags, stage directions, sound effects, or meta-commentary. The prompt SHALL clarify that TTS-supported cues (as described in the TTS formatting section, when present) ARE allowed inside speaker tags.

The prompt SHALL include the following engagement techniques:

**Hook opening:** The prompt SHALL instruct the interviewer to NOT start with a standard welcome. Instead, the interviewer SHALL open with a provocative statement, surprising fact, or compelling question drawn from the most interesting article of the day, then transition into the regular introduction.

**Front-load best story:** The prompt SHALL instruct the LLM to lead with the most compelling or surprising article, not the order they appear in the summaries.

**Curiosity hooks:** The prompt SHALL instruct the interviewer to use rhetorical questions and teaser hooks before transitions (e.g., "But here's where it gets really interesting...", "So why should we care?"). These create micro-curiosity loops that pull listeners forward.

**Mid-roll callbacks:** The prompt SHALL instruct speakers to reference earlier topics later in the episode to create narrative cohesion (e.g., "Remember that framework we discussed earlier? Well, this connects directly..."). The prompt SHALL require at least one callback per episode.

**Short segments with signposting:** The prompt SHALL instruct speakers to keep individual topic segments concise (roughly 60-90 seconds each). The prompt SHALL instruct the interviewer to use clear verbal signposts so listeners always know where they are.

**Natural interruptions:** The prompt SHALL instruct the interviewer to occasionally interrupt the expert mid-topic — not at the end of a complete explanation, but while the expert is still building their point. Each expert turn SHALL be kept to 3-5 sentences max, then the interviewer jumps in with a reaction, follow-up question, or interjection. The expert then continues in their next turn. The prompt SHALL target 3-4 interruptions per episode, spread across different topics.

**Emphasis on important announcements:** The prompt SHALL instruct speakers to convey significance when covering major announcements or surprising developments — using emphatic language, exclamation marks, and brief pauses to let important news land. Not everything is exciting; the prompt SHALL instruct speakers to save the energy for what truly stands out.

The prompt SHALL include transition guidance to ensure natural speaker handovers:
- The prompt SHALL instruct speakers to NOT start turns with bare name addresses (e.g., "Jarno, the market...")
- The prompt SHALL instruct the interviewer to use conversational bridges — reactions, follow-ups, or connectors — before transitioning to a new question or topic (e.g., "That's a great point. Now I'm curious about...", "Interesting. Speaking of which...")
- The prompt SHALL instruct that when names are used, they appear mid-sentence or at the end of questions (e.g., "What do you make of this, Jarno?") rather than as bare turn openers
- The prompt SHALL instruct speakers to vary their transition patterns — not every handover needs to use the other speaker's name

When `speakerNames` is provided on the podcast, the prompt SHALL instruct the LLM to use these names in conversation (e.g., "So Bob, what happened this week?"). When `speakerNames` is not provided, the prompt SHALL instruct speakers to address each other without names.

When a previous episode recap is provided, the prompt SHALL include a "Previous episode context" section. The prompt SHALL instruct the interviewer to naturally reference the previous episode when bridging topics. When topics are unrelated, the interviewer SHALL briefly mention the previous episode in the opening.

When `customInstructions` is provided on the podcast, the prompt SHALL append them.

#### Scenario: Interview script generated with two speakers
- **WHEN** the `InterviewComposer` composes a script for a podcast with `ttsVoices: {"interviewer": "id1", "expert": "id2"}`
- **THEN** the output contains alternating `<interviewer>` and `<expert>` tags with the interviewer asking questions and the expert delivering content

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

#### Scenario: Custom instructions included
- **WHEN** the podcast has `customInstructions: "Focus on practical implications"`
- **THEN** the prompt includes these instructions

#### Scenario: Hook opening instead of standard welcome
- **WHEN** the `InterviewComposer` generates a script
- **THEN** the prompt instructs the interviewer to open with a provocative statement, surprising fact, or compelling question rather than a standard welcome

#### Scenario: Best story is front-loaded
- **WHEN** the `InterviewComposer` generates a script from 5 articles
- **THEN** the prompt instructs the LLM to lead with the most compelling or surprising article regardless of input order

#### Scenario: Curiosity hooks used in transitions
- **WHEN** the `InterviewComposer` generates a script with multiple topics
- **THEN** the prompt instructs the interviewer to use rhetorical questions and teaser hooks before transitions

#### Scenario: Mid-roll callbacks create cohesion
- **WHEN** the `InterviewComposer` generates a script with 4+ articles
- **THEN** the prompt instructs speakers to reference earlier topics at least once later in the episode

#### Scenario: Short segments with signposting
- **WHEN** the `InterviewComposer` generates a script
- **THEN** the prompt instructs keeping topic segments to roughly 60-90 seconds each with clear verbal signposts

#### Scenario: Natural mid-sentence interruptions
- **WHEN** the `InterviewComposer` generates a script
- **THEN** the prompt instructs the interviewer to interrupt the expert mid-topic 3-4 times per episode with brief reactions or follow-up questions

#### Scenario: Emphasis on important announcements
- **WHEN** the `InterviewComposer` generates a script covering a major announcement
- **THEN** the prompt instructs speakers to convey significance through emphatic language, exclamation marks, and pacing

#### Scenario: TTS cues allowed inside speaker tags
- **WHEN** the `InterviewComposer` generates a script with Inworld TTS guidelines
- **THEN** the prompt clarifies that TTS-supported cues (e.g., `[laugh]`, `*emphasis*`) ARE allowed inside speaker tags

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

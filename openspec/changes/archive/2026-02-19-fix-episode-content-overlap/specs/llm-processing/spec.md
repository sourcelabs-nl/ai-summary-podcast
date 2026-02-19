## MODIFIED Requirements

### Requirement: Briefing script composition
The system SHALL compose all relevant articles into a single coherent briefing script. For each article, the composer SHALL use the article's `summary` if available, or the article's `body` if no summary exists (short articles). The prompt SHALL include the current date (human-readable format), the podcast name, and the podcast topic as context. The prompt SHALL instruct the LLM to mention the podcast name, topic, and current date in the introduction. Each article in the summary block SHALL include the source domain name (extracted from the article URL) and the author name when available (format: `[domain, by Author]` or `[domain]` when author is null). The prompt SHALL instruct the LLM to naturally attribute information to its source and credit original authors when known — without over-citing. The prompt SHALL instruct the LLM to use natural spoken language (not written style), include transitions between topics, and target approximately the configured word count (default: 1500 words for ~10 minutes of audio). The prompt SHALL explicitly prohibit bracketed section headers (e.g., `[Opening]`, `[Closing]`, `[Transition]`), stage directions, sound effects, and any other non-spoken text. After receiving the LLM response, the system SHALL sanitize the script by removing any lines consisting solely of bracketed headers (pattern: lines matching `[...]` with no other content). This step SHALL use a more capable model (configurable, e.g., `anthropic/claude-sonnet-4-20250514`). When the podcast's language is not English, the prompt SHALL instruct the LLM to write the entire script in the specified language. The current date in the prompt SHALL be formatted using the locale corresponding to the podcast's language.

The prompt SHALL include a grounding instruction requiring the LLM to ONLY discuss topics, facts, and claims that are directly present in the provided article summaries. The LLM SHALL NOT introduce information from its own training knowledge. When few articles are provided, the LLM SHALL produce a proportionally shorter script rather than padding with external knowledge.

This grounding instruction SHALL be applied to all three composer variants: `BriefingComposer`, `DialogueComposer`, and `InterviewComposer`.

#### Scenario: Composer uses summary for scored articles
- **WHEN** an article has a non-null `summary` from the score+summarize stage
- **THEN** the composer uses the summary in the article block

#### Scenario: Composer uses body for unsummarized articles
- **WHEN** an article has a null `summary`
- **THEN** the composer uses the full article body in the article block

#### Scenario: Multiple articles composed into briefing
- **WHEN** 5 relevant articles are passed to the composition step
- **THEN** a single briefing script is produced that covers all 5 topics with spoken-language transitions

#### Scenario: Script does not contain bracketed section headers
- **WHEN** a briefing script is generated
- **THEN** the output SHALL NOT contain any bracketed section headers such as `[Opening]`, `[Transition]`, or `[Closing]`

#### Scenario: Non-English briefing script
- **WHEN** a briefing is composed for a podcast with language `"nl"` (Dutch)
- **THEN** the LLM prompt instructs the model to write the entire script in Dutch

#### Scenario: LLM does not introduce external knowledge
- **WHEN** the composer receives 3 articles about topics A, B, and C
- **THEN** the generated script discusses only topics A, B, and C without introducing additional topics from the LLM's training data

#### Scenario: Short script for few articles
- **WHEN** the composer receives only 2 articles
- **THEN** the generated script is proportionally shorter than when receiving 10 articles, rather than padded with external content

# Capability: LLM Processing (Delta)

## MODIFIED Requirements

### Requirement: Briefing script composition
The system SHALL compose all relevant articles into a single coherent briefing script. The composer SHALL select article content based on a full-body threshold:
- When the number of relevant articles is **below** the podcast's `fullBodyThreshold` (default: 5, configurable per-podcast with global fallback), the composer SHALL use the full `article.body` for all articles.
- When the number of relevant articles is **at or above** the threshold, the composer SHALL use `article.summary` if available, or `article.body` if no summary exists.

The prompt SHALL include the current date (human-readable format), the podcast name, and the podcast topic as context. The prompt SHALL instruct the LLM to mention the podcast name, topic, and current date in the introduction. Each article in the content block SHALL include the source domain name (extracted from the article URL) and the author name when available (format: `[domain, by Author]` or `[domain]` when author is null). The prompt SHALL instruct the LLM to naturally attribute information to its source and credit original authors when known — without over-citing. The prompt SHALL instruct the LLM to use natural spoken language (not written style), include transitions between topics, and target approximately the configured word count (default: 1500 words for ~10 minutes of audio). The prompt SHALL explicitly prohibit bracketed section headers (e.g., `[Opening]`, `[Closing]`, `[Transition]`), stage directions, sound effects, and any other non-spoken text. After receiving the LLM response, the system SHALL sanitize the script by removing any lines consisting solely of bracketed headers (pattern: lines matching `[...]` with no other content). This step SHALL use a more capable model (configurable, e.g., `anthropic/claude-sonnet-4-20250514`). When the podcast's language is not English, the prompt SHALL instruct the LLM to write the entire script in the specified language. The current date in the prompt SHALL be formatted using the locale corresponding to the podcast's language.

The prompt SHALL include a grounding instruction requiring the LLM to ONLY discuss topics, facts, and claims that are directly present in the provided article summaries. The LLM SHALL NOT introduce information from its own training knowledge. When few articles are provided, the LLM SHALL produce a proportionally shorter script rather than padding with external knowledge.

When the podcast has a non-null `sponsor` field, the prompt SHALL instruct the LLM to include a sponsor message immediately after the introduction using the configured sponsor name and message (e.g., "This podcast is brought to you by {sponsor.name} — {sponsor.message}."). The prompt SHALL also instruct the LLM to mention the sponsor name in the sign-off/outro. When the podcast has no sponsor configured (null), the prompt SHALL NOT include any sponsor-related instructions.

The full-body threshold behavior SHALL be applied to all three composer variants: `BriefingComposer`, `DialogueComposer`, and `InterviewComposer`.

#### Scenario: Few articles triggers full body usage
- **WHEN** 3 relevant articles are passed to the composer and the podcast's `fullBodyThreshold` is 5
- **THEN** the composer uses the full `article.body` for all 3 articles in the prompt

#### Scenario: Many articles uses summaries
- **WHEN** 8 relevant articles are passed to the composer and the podcast's `fullBodyThreshold` is 5
- **THEN** the composer uses `article.summary` (or `article.body` as fallback) for each article

#### Scenario: Threshold at exact boundary uses summaries
- **WHEN** 5 relevant articles are passed to the composer and the podcast's `fullBodyThreshold` is 5
- **THEN** the composer uses `article.summary` (or `article.body` as fallback), since the count is not below the threshold

#### Scenario: Per-podcast threshold override
- **WHEN** a podcast has `fullBodyThreshold` set to 3 and 4 relevant articles are passed
- **THEN** the composer uses summaries, since 4 is not below 3

#### Scenario: Composer uses summary for scored articles
- **WHEN** article count is at or above the threshold and an article has a non-null `summary`
- **THEN** the composer uses the summary in the article block

#### Scenario: Composer uses body for unsummarized articles
- **WHEN** article count is at or above the threshold and an article has a null `summary`
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

#### Scenario: Sponsor message included when configured
- **WHEN** a podcast has `sponsor: {"name": "source-labs", "message": "experts in agentic software development"}` and any composer generates a script
- **THEN** the prompt instructs the LLM to include "This podcast is brought to you by source-labs — experts in agentic software development" after the introduction

#### Scenario: Sponsor mentioned in sign-off when configured
- **WHEN** a podcast has a non-null sponsor and any composer generates a script
- **THEN** the prompt instructs the LLM to mention the sponsor name in the sign-off/outro

#### Scenario: No sponsor instructions when not configured
- **WHEN** a podcast has `sponsor` set to null and any composer generates a script
- **THEN** the prompt does not include any sponsor-related instructions

#### Scenario: Sponsor name used as-is from configuration
- **WHEN** a podcast has `sponsor: {"name": "Acme Corp", "message": "innovating tomorrow"}`
- **THEN** the prompt uses "Acme Corp" as the sponsor name (no hardcoded name)

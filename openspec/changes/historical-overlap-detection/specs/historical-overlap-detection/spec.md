## ADDED Requirements

### Requirement: Overlap detection between candidate articles and recent episodes
The system SHALL detect candidate articles that semantically overlap with articles already included in recent episodes for the same podcast. The detection SHALL use a single LLM call (filter model) that receives candidate article summaries and recent episode article summaries, and returns a list of candidate article IDs to exclude. The lookback window SHALL be configurable: `overlapLookbackEpisodes` on the `Podcast` entity (nullable, per-podcast override) with a global default via `app.llm.overlap-lookback-episodes` (default: 5). The system SHALL query articles linked to the N most recent episodes with `status = 'GENERATED'` for the podcast via the `episode_articles` join table.

#### Scenario: Overlapping article excluded
- **WHEN** candidate article A summarizes "OpenAI releases GPT-5" and episode 42 (generated 2 days ago) already contains an article summarizing "OpenAI launches GPT-5"
- **THEN** candidate article A is flagged as overlapping and excluded from composition

#### Scenario: Follow-up story with new information is kept
- **WHEN** candidate article A summarizes "OpenAI GPT-5 pricing announced at $30/month" and episode 42 contains an article about "OpenAI releases GPT-5"
- **THEN** candidate article A is NOT flagged as overlapping because it contains genuinely new information

#### Scenario: No recent episodes — overlap check skipped
- **WHEN** the pipeline runs for a podcast with no episodes with `status = 'GENERATED'`
- **THEN** the overlap detection step is skipped entirely and all candidate articles proceed to composition

#### Scenario: No candidate articles after scoring — overlap check skipped
- **WHEN** the pipeline runs but no articles meet the relevance threshold
- **THEN** the overlap detection step is skipped (there are no candidates to check)

#### Scenario: Custom lookback window per podcast
- **WHEN** a podcast has `overlapLookbackEpisodes = 10` and the global default is 5
- **THEN** the overlap check queries articles from the 10 most recent GENERATED episodes

#### Scenario: All candidates overlap — no briefing generated
- **WHEN** all candidate articles are flagged as overlapping with recent episode content
- **THEN** no composition LLM call is made and no briefing is generated

### Requirement: Overlap detection LLM prompt
The overlap detection LLM call SHALL receive a structured prompt with two sections: "Previously published content" (article summaries grouped by episode, most recent first) and "New candidate articles" (article ID + summary for each candidate). The prompt SHALL instruct the LLM to return a JSON array of objects, each containing the candidate article `id` and a brief `reason` for the overlap. The prompt SHALL explicitly state that follow-up stories with genuinely new information are NOT overlaps. The LLM SHALL use the filter model (same model as the scoring stage).

#### Scenario: LLM returns overlap results as JSON
- **WHEN** the overlap detection call completes with 2 overlapping candidates out of 5
- **THEN** the response contains a JSON array with 2 objects: `[{"id": 101, "reason": "Same GPT-5 launch announcement"}, {"id": 103, "reason": "Same EU AI Act coverage"}]`

#### Scenario: LLM returns empty array when no overlaps found
- **WHEN** the overlap detection call completes and no candidates overlap with recent content
- **THEN** the response contains an empty JSON array `[]`

#### Scenario: Token usage tracked for overlap detection
- **WHEN** the overlap detection LLM call uses 800 input tokens and 50 output tokens
- **THEN** the token usage is included in the `PipelineResult` totals

### Requirement: Overlap detection logging
The system SHALL log which articles were excluded due to overlap, including the article ID, title, and the overlap reason from the LLM. The log level SHALL be INFO.

#### Scenario: Overlapping articles logged
- **WHEN** 2 articles are excluded due to overlap
- **THEN** the system logs an INFO message for each: `[LLM] Excluding article {id} '{title}' — overlaps with recent episode: {reason}`

#### Scenario: No overlaps found logged
- **WHEN** the overlap check finds no overlaps among 5 candidates
- **THEN** the system logs: `[LLM] Overlap check passed: 0 of 5 candidates overlap with recent episodes`

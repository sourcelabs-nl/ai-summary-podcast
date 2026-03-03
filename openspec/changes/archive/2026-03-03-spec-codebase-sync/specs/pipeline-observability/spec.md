## MODIFIED Requirements

### Requirement: Article processing batch progress logging
The system SHALL log progress during article processing, showing the current item number and total count at INFO level. Each article log message SHALL include the source domain and path.

#### Scenario: Batch progress during article processing
- **WHEN** the ArticleScoreSummarizer processes an article
- **THEN** the log includes the source domain+path (e.g., `[LLM] Article 'Title' scored 8 — summary: 922 chars (source: techcrunch.com/feed)`)

#### Scenario: Article processing batch summary
- **WHEN** the ArticleProcessor completes processing all articles in a batch
- **THEN** an info log is emitted with the total count, number of relevant articles, and elapsed time (e.g., `[LLM] Article processing complete — 12 articles in 45.2s (8 relevant)`)

### Requirement: Briefing composition start and timing logging
The system SHALL log the start and completion of briefing script composition at INFO level. The completion log MUST include elapsed time. All composition log messages SHALL include the podcast name in the format `'Name' (uuid)`.

#### Scenario: Briefing composition lifecycle logging
- **WHEN** the BriefingComposer, DialogueComposer, or InterviewComposer starts and completes composing a script
- **THEN** an info log is emitted at start using style-specific prefixes (e.g., `[LLM] Composing briefing from 5 articles`, `[LLM] Composing dialogue from 5 articles`, `[LLM] Composing interview from 5 articles`) and at completion with elapsed time and word count

## MODIFIED Requirements

### Requirement: Pipeline run logging with elapsed time
The system SHALL log the start and completion of each briefing generation pipeline run at INFO level. The completion log MUST include the total elapsed time. All log messages that reference a podcast SHALL include the podcast name in the format `'Name' (uuid)`.

#### Scenario: Full pipeline run logging
- **WHEN** the briefing generation scheduler triggers a pipeline run for a podcast
- **THEN** an info log is emitted at the start (e.g., `[Pipeline] Starting briefing generation for podcast 'AI Daily News' (6aa0af72-...)`) and at completion (e.g., `[Pipeline] Briefing generation complete for podcast 'AI Daily News' (6aa0af72-...): episode 19 — total 45.2s`)

#### Scenario: Pipeline run with no articles
- **WHEN** the pipeline run completes early because no relevant articles were found
- **THEN** the completion log MUST still be emitted with the podcast name, elapsed time, and reason for early exit

### Requirement: Article processing batch progress logging
The system SHALL log progress during article processing, showing the current item number and total count at INFO level. Each article log message SHALL include the source domain and path.

#### Scenario: Batch progress during article processing
- **WHEN** the ArticleScoreSummarizer processes an article
- **THEN** the log includes the source domain+path (e.g., `[LLM] Article 'Title' scored 8 — summary: 922 chars (source: techcrunch.com/feed)`)

### Requirement: Briefing composition start and timing logging
The system SHALL log the start and completion of briefing script composition at INFO level. The completion log MUST include elapsed time. All composition log messages SHALL include the podcast name in the format `'Name' (uuid)`.

#### Scenario: Briefing composition lifecycle logging
- **WHEN** the BriefingComposer, DialogueComposer, or InterviewComposer starts and completes composing a script
- **THEN** an info log is emitted at start (e.g., `[LLM] Composing briefing from 5 articles for podcast 'AI Daily News' (6aa0af72-...)`) and at completion with elapsed time and word count

### Requirement: TTS pipeline start logging
The system SHALL log the start of TTS audio generation at INFO level before processing begins. All TTS log messages that reference a podcast SHALL include the podcast name in the format `'Name' (uuid)`.

#### Scenario: TTS pipeline start event
- **WHEN** the TTS pipeline begins generating audio for an episode
- **THEN** an info log is emitted including the podcast name (e.g., `[TTS] Starting audio generation for podcast 'AI Daily News' (6aa0af72-...) (provider: openai)`)

## ADDED Requirements

### Requirement: Source label extraction utility
The system SHALL provide a utility function `extractDomainAndPath(url: String): String` that extracts the domain and path from a URL, stripping the scheme and `www.` prefix.

#### Scenario: Standard URL extraction
- **WHEN** `extractDomainAndPath("https://www.techcrunch.com/feed")` is called
- **THEN** the result is `"techcrunch.com/feed"`

#### Scenario: URL with no path
- **WHEN** `extractDomainAndPath("https://example.com")` is called
- **THEN** the result is `"example.com"`

#### Scenario: Malformed URL fallback
- **WHEN** `extractDomainAndPath("not-a-url")` is called
- **THEN** the result is `"not-a-url"` (falls back to input)

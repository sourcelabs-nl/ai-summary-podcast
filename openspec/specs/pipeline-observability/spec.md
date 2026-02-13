## Purpose

Structured info-level logging across all pipeline stages with timing, progress counters, and run summaries for operational visibility.

## Requirements

### Requirement: Pipeline run logging with elapsed time
The system SHALL log the start and completion of each briefing generation pipeline run at INFO level. The completion log MUST include the total elapsed time.

#### Scenario: Full pipeline run logging
- **WHEN** the briefing generation scheduler triggers a pipeline run for a podcast
- **THEN** an info log is emitted at the start (e.g., `[Pipeline] Starting briefing generation for podcast {id}`) and at completion (e.g., `[Pipeline] Briefing generation complete for podcast {id} — total {elapsed}`)

#### Scenario: Pipeline run with no articles
- **WHEN** the pipeline run completes early because no relevant articles were found
- **THEN** the completion log MUST still be emitted with the elapsed time and reason for early exit

### Requirement: Article processing batch progress logging
The system SHALL log progress during article processing, showing the current item number and total count at INFO level.

#### Scenario: Batch progress during article processing
- **WHEN** the ArticleProcessor processes a batch of articles
- **THEN** each article logs its position in the batch (e.g., `[LLM] Processing article 3/12: '{title}'`)

#### Scenario: Article processing batch summary
- **WHEN** the ArticleProcessor completes processing all articles in a batch
- **THEN** an info log is emitted with the total count, number of relevant articles, and elapsed time (e.g., `[LLM] Article processing complete — 12 articles in 45.2s (8 relevant)`)

### Requirement: Briefing composition start and timing logging
The system SHALL log the start and completion of briefing script composition at INFO level. The completion log MUST include elapsed time.

#### Scenario: Briefing composition lifecycle logging
- **WHEN** the BriefingComposer starts and completes composing a briefing script
- **THEN** an info log is emitted at start (e.g., `[LLM] Composing briefing from {n} articles`) and at completion with elapsed time and word count

### Requirement: TTS pipeline start logging
The system SHALL log the start of TTS audio generation at INFO level before processing begins.

#### Scenario: TTS pipeline start event
- **WHEN** the TTS pipeline begins generating audio for an episode
- **THEN** an info log is emitted indicating the start of TTS generation (e.g., `[TTS] Starting audio generation for podcast {id}`)

### Requirement: Source polling visibility at info level
The system SHALL log source polling scheduler activity at INFO level so it is visible in default log output.

#### Scenario: Polling check is visible at default log level
- **WHEN** the source polling scheduler runs its scheduled check
- **THEN** an info-level log is emitted showing how many sources are being checked (e.g., `[Polling] Checking {n} enabled sources`)

### Requirement: Consistent log message prefix format
All pipeline progress log messages SHALL use a bracketed stage prefix to identify which pipeline stage produced the message.

#### Scenario: Log messages use stage prefixes
- **WHEN** any pipeline stage emits a progress log
- **THEN** the message starts with a bracketed prefix: `[Pipeline]`, `[LLM]`, `[TTS]`, or `[Polling]`

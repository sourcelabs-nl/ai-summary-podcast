## MODIFIED Requirements

### Requirement: Scheduled source polling
The system SHALL poll each enabled source on a configurable schedule using Spring's `@Scheduled`. A `SourcePollingScheduler` SHALL run on a fixed interval, iterate over all enabled sources, and poll each source whose effective poll interval has elapsed since its last poll. The effective poll interval SHALL account for exponential backoff: for sources with `consecutiveFailures > 0`, the interval is `pollIntervalMinutes × 2^consecutiveFailures`, capped at `app.source.max-backoff-hours` converted to minutes. For sources with `consecutiveFailures = 0`, the normal `pollIntervalMinutes` is used. For source types that require per-user API keys (e.g., `"twitter"`), the scheduler SHALL resolve the podcast's owner user ID and pass it to the `SourcePoller`.

#### Scenario: Source polled when interval has elapsed
- **WHEN** the scheduler runs and a source's effective poll interval has elapsed since its `last_polled` timestamp
- **THEN** the source is polled for new content

#### Scenario: Source skipped when interval has not elapsed
- **WHEN** the scheduler runs and a source was polled less than its effective poll interval ago
- **THEN** the source is skipped in this polling cycle

#### Scenario: Disabled source never polled
- **WHEN** the scheduler runs and a source has `enabled: false`
- **THEN** the source is not polled

#### Scenario: Twitter source polled with user context
- **WHEN** the scheduler runs and a source with `type: "twitter"` is due for polling
- **THEN** the scheduler resolves the podcast owner's user ID and passes it to the poller so the fetcher can look up the user's X API key

#### Scenario: Source with failures uses backoff interval
- **WHEN** the scheduler runs and a source has `consecutiveFailures = 2` and `pollIntervalMinutes = 60`
- **THEN** the source is only polled if at least 240 minutes (60 × 2²) have elapsed since `lastPolled`

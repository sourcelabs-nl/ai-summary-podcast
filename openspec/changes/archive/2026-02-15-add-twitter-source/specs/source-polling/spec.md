## MODIFIED Requirements

### Requirement: Scheduled source polling
The system SHALL poll each enabled source on a configurable schedule using Spring's `@Scheduled`. A `SourcePollingScheduler` SHALL run on a fixed interval, iterate over all enabled sources, and poll each source whose individual `pollIntervalMinutes` has elapsed since its last poll. For source types that require per-user API keys (e.g., `"twitter"`), the scheduler SHALL resolve the podcast's owner user ID and pass it to the `SourcePoller`.

#### Scenario: Source polled when interval has elapsed
- **WHEN** the scheduler runs and a source's `pollIntervalMinutes` has elapsed since its `last_polled` timestamp
- **THEN** the source is polled for new content

#### Scenario: Source skipped when interval has not elapsed
- **WHEN** the scheduler runs and a source was polled less than `pollIntervalMinutes` ago
- **THEN** the source is skipped in this polling cycle

#### Scenario: Disabled source never polled
- **WHEN** the scheduler runs and a source has `enabled: false`
- **THEN** the source is not polled

#### Scenario: Twitter source polled with user context
- **WHEN** the scheduler runs and a source with `type: "twitter"` is due for polling
- **THEN** the scheduler resolves the podcast owner's user ID and passes it to the poller so the fetcher can look up the user's X API key

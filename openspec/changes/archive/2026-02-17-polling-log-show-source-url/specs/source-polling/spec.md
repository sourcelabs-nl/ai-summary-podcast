## MODIFIED Requirements

### Requirement: Scheduled source polling
The system SHALL poll each enabled source on a configurable schedule using Spring's `@Scheduled`. A `SourcePollingScheduler` SHALL run on a fixed interval, iterate over all enabled sources, and poll each source whose effective poll interval has elapsed since its last poll. The effective poll interval SHALL account for exponential backoff: for sources with `consecutiveFailures > 0`, the interval is `pollIntervalMinutes × 2^consecutiveFailures`, capped at `app.source.max-backoff-hours` converted to minutes. For sources with `consecutiveFailures = 0`, the normal `pollIntervalMinutes` is used. For source types that require per-user API keys (e.g., `"twitter"`), the scheduler SHALL resolve the podcast's owner user ID and pass it to the `SourcePoller`.

The scheduler's `pollSources()` method SHALL be a `suspend fun`, using Spring 6.1+'s native coroutine support for `@Scheduled` methods. Due sources SHALL be grouped by URL host (extracted via `java.net.URI(url).host`). Each host group SHALL be polled as a parallel coroutine under `supervisorScope`, with sequential polling and configurable delays within each group (as defined by the `poll-rate-limiting` capability).

Sources with `lastPolled = null` (never polled) SHALL receive startup jitter before being checked for due status (as defined by the `poll-rate-limiting` capability).

All `[Polling]` log messages in `SourcePoller` that identify a source SHALL use `source.url` instead of `source.id` so that operators can identify sources without a database lookup.

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

#### Scenario: Host groups polled in parallel
- **WHEN** the scheduler runs and due sources span multiple hosts
- **THEN** each host group is polled concurrently as a separate coroutine under `supervisorScope`

#### Scenario: Scheduler method is a suspend function
- **WHEN** the scheduler tick fires
- **THEN** `pollSources()` executes as a Kotlin `suspend fun` using Spring's native coroutine scheduling support

#### Scenario: Log messages show source URL
- **WHEN** a source is polled and log messages are emitted
- **THEN** the log messages identify the source by its URL (not its UUID)

## Context

Source polling currently catches all exceptions in `SourcePoller.poll()` and updates `lastPolled` regardless of success or failure. This means a failed source is retried at its normal `pollIntervalMinutes` interval indefinitely. There is no failure tracking, no backoff, and no mechanism to disable sources that are permanently broken (e.g. deleted RSS feeds returning 404).

The `Source` entity has `enabled` and `lastPolled` fields but no failure-related state. The `SourcePollingScheduler` checks `lastPolled + pollIntervalMinutes` to decide when to poll.

## Goals / Non-Goals

**Goals:**
- Reduce noise from repeatedly polling broken sources
- Automatically disable sources that are permanently unreachable
- Provide visibility into why a source was disabled
- Make failure thresholds configurable

**Non-Goals:**
- Alerting/notifications when sources are disabled (future work)
- Per-source-type backoff strategies (all types use the same logic)
- Health check endpoint for source status
- Automatic re-enabling of disabled sources

## Decisions

### Decision 1: Failure state on the Source entity (not a separate table)

Add `consecutiveFailures`, `lastFailureType`, and `disabledReason` directly to the `Source` entity.

**Why not a separate table?** Failure state is tightly coupled to the source — it's read and written on every poll cycle. A separate table adds a join and transactional complexity for no benefit. The three new columns are lightweight.

### Decision 2: Compute next-poll-after from existing fields (no stored field)

The backoff delay is computed as: `lastPolled + pollIntervalMinutes × 2^consecutiveFailures`, capped at `maxBackoffHours`. This is derived from `lastPolled`, `pollIntervalMinutes`, and `consecutiveFailures` — no need to store a `nextPollAfter` column.

**Why?** Fewer stored fields, no risk of stored timing getting stale or out of sync. The computation is trivial.

### Decision 3: Error classification in SourcePoller via a sealed class

Introduce a `PollFailure` sealed class with two subtypes: `Transient` and `Permanent`. The `SourcePoller` classifies exceptions after catching them:

| Exception / Status | Classification |
|---|---|
| `HttpClientErrorException` with 404, 410 | Permanent |
| `HttpClientErrorException` with 401, 403 | Permanent |
| `UnknownHostException` | Permanent |
| `HttpClientErrorException` with 429 | Transient |
| `HttpServerErrorException` (5xx) | Transient |
| `SocketTimeoutException`, `ConnectException` | Transient |
| RSS/XML parse exceptions | Transient |
| All other exceptions | Transient |

**Why sealed class?** It's idiomatic Kotlin, exhaustive when used in `when` expressions, and easily extensible.

### Decision 4: Backoff formula — exponential with cap

```
backoffMinutes = min(pollIntervalMinutes × 2^consecutiveFailures, maxBackoffHours × 60)
```

Example with `pollIntervalMinutes = 60`:
- Failure 1: 120 min (2h)
- Failure 2: 240 min (4h)
- Failure 3: 480 min (8h)
- Failure 4: 960 min (16h)
- Failure 5+: 1440 min (24h, capped)

### Decision 5: Reset failure state on success and on manual re-enable

A successful poll resets `consecutiveFailures` to 0 and clears `lastFailureType`. When a user re-enables a disabled source via the API, `consecutiveFailures`, `lastFailureType`, and `disabledReason` are all cleared so the source gets a fresh start.

### Decision 6: Auto-disable only for permanent failures

Transient failures use backoff indefinitely (capped at 24h) but never auto-disable. Only permanent failures increment toward the `maxFailures` threshold. This prevents temporarily flaky sources from being disabled.

## Risks / Trade-offs

- **[Misclassification]** A temporary DNS issue could be classified as permanent and count toward auto-disable → Mitigated by the 5-failure threshold with exponential backoff between attempts, giving time for transient DNS issues to resolve
- **[Silent disabling]** Users may not notice a source was auto-disabled → Mitigated by storing `disabledReason` on the source, visible via API. Future work could add notifications
- **[Backoff too aggressive]** Exponential backoff could delay recovery detection for transient issues → Mitigated by the 24h cap; worst case is a 24h delay in detecting recovery

## Migration Plan

1. Add Flyway migration `V18__add_source_failure_tracking.sql` with three new columns (all nullable/defaulted, non-breaking)
2. Deploy — existing sources have `consecutiveFailures = 0`, behave normally
3. No rollback complexity — new columns are simply ignored if code is rolled back
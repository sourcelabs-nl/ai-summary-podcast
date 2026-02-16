## Why

Source polling currently has no failure tracking. When a source fails (e.g. RSS feed returns 404, website is down), the system retries at the same interval indefinitely, generating noise in logs and wasting resources on dead sources. There is no way for the system to back off from flaky sources or auto-disable permanently broken ones.

## What Changes

- Add exponential backoff for sources that fail repeatedly — poll interval doubles with each consecutive failure, capped at 24 hours
- Classify errors into two tracks: **transient** (timeout, 500, rate limit, parse errors) and **permanent** (404, 410, DNS failure, 401, 403)
- Transient errors get backoff but the source stays enabled indefinitely
- Permanent errors get backoff AND auto-disable the source after a configurable number of consecutive failures (default: 5)
- Store a human-readable `disabledReason` on the source when auto-disabled
- Add new `Source` entity fields: `consecutiveFailures`, `lastFailureType`, `disabledReason`
- Add new configuration properties: `app.source.max-failures` (default 5), `app.source.max-backoff-hours` (default 24)
- Re-enabling a source via API clears failure tracking state

## Capabilities

### New Capabilities
- `source-polling-backoff`: Exponential backoff and auto-disable logic for failing sources, including error classification and failure tracking

### Modified Capabilities
- `source-polling`: Add failure tracking fields to the polling cycle — the scheduler must respect backoff timing and the poller must classify and record failures

## Impact

- **Database**: New columns on `sources` table (`consecutive_failures`, `last_failure_type`, `disabled_reason`) — requires a Flyway migration
- **Source entity**: Three new fields added to the `Source` data class
- **SourcePoller**: Must classify exceptions and update failure state on the source
- **SourcePollingScheduler**: Must check `nextPollAfter` (computed from backoff) instead of just `lastPolled + pollInterval`
- **SourceService**: Re-enabling a source must reset failure tracking fields
- **AppProperties**: New `maxFailures` and `maxBackoffHours` fields on `SourceProperties`
- **API**: Source response now includes `consecutiveFailures`, `lastFailureType`, `disabledReason` (non-breaking addition)
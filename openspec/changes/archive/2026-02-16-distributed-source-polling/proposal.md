## Why

When multiple sources share the same host (e.g. several Nitter RSS feeds on the same instance), they are all polled back-to-back in a tight sequential loop. This causes request bursts that trigger rate limiting on free/community-run services. On startup or after downtime, all sources become due simultaneously, amplifying the problem. The scheduler also runs sequentially — a slow host blocks polling of all other hosts.

## What Changes

- Refactor `SourcePollingScheduler` to group due sources by host and poll host groups in parallel using Kotlin coroutines with `supervisorScope` (one host failure does not cancel others)
- Within each host group, poll sources sequentially with a configurable delay between requests
- Add per-source `pollDelaySeconds` field for explicit override
- Add global config for default poll delay per source type (`app.source.poll-delay-seconds.*`)
- Add global config for host-specific delay overrides (`app.source.host-overrides.*`)
- Add startup jitter: sources with no `lastPolled` get a random initial delay within their poll interval to prevent first-boot bursts
- Convert `@Scheduled` poll method to `suspend fun` (Spring 6.1+ coroutine support)

## Capabilities

### New Capabilities
- `poll-rate-limiting`: Configurable per-host and per-source-type request spacing to prevent rate limit violations on free/community-run services

### Modified Capabilities
- `source-polling`: Scheduler changes from sequential loop to parallel host-grouped coroutines with startup jitter
- `source-config`: New `pollDelaySeconds` field on Source entity

## Impact

- `SourcePollingScheduler` — major refactor: coroutines, host grouping, delay logic
- `Source` entity — new nullable `pollDelaySeconds` column
- `AppProperties` — new `poll-delay-seconds` and `host-overrides` config sections
- Database migration — add `poll_delay_seconds` column to `sources` table
- Dependencies — Kotlin coroutines library (if not already present)
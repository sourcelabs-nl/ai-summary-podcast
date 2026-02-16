## 1. Dependencies and Configuration

- [x] 1.1 Add `kotlinx-coroutines-core` and `kotlinx-coroutines-reactor` dependencies to `pom.xml`
- [x] 1.2 Extend `SourceProperties` in `AppProperties.kt` with `pollDelaySeconds: Map<String, Int>` and `hostOverrides: Map<String, HostOverride>` (new `HostOverride` data class with `pollDelaySeconds: Int`)
- [x] 1.3 Add `pollDelaySeconds: Int?` field to the `Source` entity
- [x] 1.4 Create database migration `V21__add_poll_delay_seconds_to_sources.sql` adding nullable `poll_delay_seconds` column to `sources` table

## 2. Delay Resolution

- [x] 2.1 Create `PollDelayResolver` component that resolves delay for a source using the three-layer chain: per-source → host-override → type-default → 0
- [x] 2.2 Write tests for `PollDelayResolver` covering all precedence levels and edge cases (unparseable URL, missing config)

## 3. Scheduler Refactor

- [x] 3.1 Implement startup jitter: in `pollSources()`, for sources with `lastPolled = null`, set a synthetic `lastPolled` timestamp of `now - random(0..pollIntervalMinutes) minutes` and persist it
- [x] 3.2 Refactor `SourcePollingScheduler.pollSources()` to `suspend fun` with host-grouped parallel polling using `supervisorScope` and `async`
- [x] 3.3 Within each host group coroutine, poll sources sequentially and apply `delay()` using the resolved delay from `PollDelayResolver`
- [x] 3.4 Use `Dispatchers.IO` for poll operations since fetchers perform blocking HTTP calls

## 4. Source API

- [x] 4.1 Update source create/update API to accept and persist `pollDelaySeconds` field

## 5. Testing

- [x] 5.1 Write tests for startup jitter: verify sources with null `lastPolled` get synthetic timestamps, verify sources with existing `lastPolled` are not modified
- [x] 5.2 Write tests for parallel host grouping: verify sources are grouped by host, verify groups run concurrently
- [x] 5.3 Write tests for `supervisorScope` isolation: verify one host group failure does not cancel others
- [x] 5.4 Write tests for delay application: verify correct delay is applied between same-host polls
- [x] 5.5 Update existing `SourcePollingSchedulerBackoffTest` to account for the new `suspend fun` signature

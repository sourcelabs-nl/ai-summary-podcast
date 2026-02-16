## 1. Database & Entity

- [x] 1.1 Add Flyway migration `V18__add_source_failure_tracking.sql` with columns: `consecutive_failures INTEGER NOT NULL DEFAULT 0`, `last_failure_type TEXT`, `disabled_reason TEXT`
- [x] 1.2 Add `consecutiveFailures`, `lastFailureType`, and `disabledReason` fields to the `Source` data class

## 2. Configuration

- [x] 2.1 Add `maxFailures` (default 5) and `maxBackoffHours` (default 24) to `SourceProperties` in `AppProperties.kt`

## 3. Error Classification

- [x] 3.1 Create `PollFailure` sealed class with `Transient` and `Permanent` subtypes, including a companion `classify(exception: Exception)` function that maps exceptions to the correct type per the spec

## 4. Core Polling Logic

- [x] 4.1 Update `SourcePoller.poll()` to classify errors on failure, increment `consecutiveFailures`, set `lastFailureType`, and auto-disable the source when consecutive permanent failures reach `maxFailures`
- [x] 4.2 Update `SourcePoller.poll()` to reset `consecutiveFailures` and `lastFailureType` to defaults on successful poll
- [x] 4.3 Update `SourcePollingScheduler` to compute effective poll interval with exponential backoff (`pollIntervalMinutes × 2^consecutiveFailures`, capped at `maxBackoffHours`)

## 5. Source Re-enable Logic

- [x] 5.1 Update `SourceService` to reset `consecutiveFailures`, `lastFailureType`, and `disabledReason` when a source is re-enabled via the API

## 6. Tests

- [x] 6.1 Write unit tests for `PollFailure.classify()` covering all error types (404, 410, 401, 403, DNS, 429, 5xx, timeout, parse error, unknown)
- [x] 6.2 Write unit tests for `SourcePoller` failure tracking: counter increment, type classification, success reset, auto-disable at threshold
- [x] 6.3 Write unit tests for `SourcePollingScheduler` backoff: verify sources with failures are skipped until backoff interval elapses, verify cap at max-backoff-hours
- [x] 6.4 Write unit test for `SourceService` re-enable: verify failure state is cleared when enabling a disabled source

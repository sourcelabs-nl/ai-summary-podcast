## ADDED Requirements

### Requirement: Error classification
The system SHALL classify polling errors into two categories: **transient** and **permanent**. Transient errors include: HTTP 429 (rate limit), HTTP 5xx (server errors), connection timeouts, socket timeouts, and XML/RSS parse failures. Permanent errors include: HTTP 404 (not found), HTTP 410 (gone), HTTP 401 (unauthorized), HTTP 403 (forbidden), and DNS resolution failures (`UnknownHostException`). All unrecognized exceptions SHALL be classified as transient.

#### Scenario: HTTP 404 classified as permanent
- **WHEN** a source poll receives an HTTP 404 response
- **THEN** the failure is classified as permanent

#### Scenario: HTTP 500 classified as transient
- **WHEN** a source poll receives an HTTP 500 response
- **THEN** the failure is classified as transient

#### Scenario: Connection timeout classified as transient
- **WHEN** a source poll times out connecting to the server
- **THEN** the failure is classified as transient

#### Scenario: DNS failure classified as permanent
- **WHEN** a source poll fails with `UnknownHostException`
- **THEN** the failure is classified as permanent

#### Scenario: HTTP 401 classified as permanent
- **WHEN** a source poll receives an HTTP 401 response
- **THEN** the failure is classified as permanent

#### Scenario: Unknown exception classified as transient
- **WHEN** a source poll fails with an unexpected exception type
- **THEN** the failure is classified as transient

### Requirement: Failure tracking on source
The system SHALL track consecutive failures on each source. The `Source` entity SHALL have a `consecutiveFailures` field (default 0), a `lastFailureType` field (nullable, values: `"transient"` or `"permanent"`), and a `disabledReason` field (nullable). On each failed poll, `consecutiveFailures` SHALL be incremented by 1 and `lastFailureType` SHALL be set to the classified error type. On a successful poll, `consecutiveFailures` SHALL be reset to 0 and `lastFailureType` SHALL be cleared to null.

#### Scenario: First failure increments counter
- **WHEN** a source poll fails for the first time with a transient error
- **THEN** `consecutiveFailures` is set to 1 and `lastFailureType` is set to `"transient"`

#### Scenario: Repeated failures increment counter
- **WHEN** a source with `consecutiveFailures = 3` fails again
- **THEN** `consecutiveFailures` is set to 4

#### Scenario: Success resets failure tracking
- **WHEN** a source with `consecutiveFailures = 3` polls successfully
- **THEN** `consecutiveFailures` is set to 0 and `lastFailureType` is set to null

### Requirement: Exponential backoff for failed sources
The system SHALL apply exponential backoff when deciding when to next poll a source that has consecutive failures. The backoff delay SHALL be computed as `pollIntervalMinutes × 2^consecutiveFailures`, capped at `app.source.max-backoff-hours` (default: 24) converted to minutes. The scheduler SHALL skip a source if the time since `lastPolled` is less than the computed backoff delay. Sources with `consecutiveFailures = 0` SHALL use the normal `pollIntervalMinutes`.

#### Scenario: First failure doubles the poll interval
- **WHEN** a source with `pollIntervalMinutes = 60` has `consecutiveFailures = 1`
- **THEN** the next poll is scheduled 120 minutes after `lastPolled`

#### Scenario: Third failure results in 8x interval
- **WHEN** a source with `pollIntervalMinutes = 60` has `consecutiveFailures = 3`
- **THEN** the next poll is scheduled 480 minutes (8 hours) after `lastPolled`

#### Scenario: Backoff capped at maximum
- **WHEN** a source with `pollIntervalMinutes = 60` has `consecutiveFailures = 5`
- **THEN** the next poll is scheduled 1440 minutes (24 hours) after `lastPolled`, not 1920 minutes

#### Scenario: Successful poll restores normal interval
- **WHEN** a source with `consecutiveFailures = 0` and `pollIntervalMinutes = 60` is due for polling
- **THEN** the scheduler polls it 60 minutes after `lastPolled` (normal interval, no backoff)

### Requirement: Auto-disable after permanent failures
The system SHALL auto-disable a source after `app.source.max-failures` (default: 5) consecutive **permanent** failures. When the threshold is reached, the source's `enabled` field SHALL be set to `false` and `disabledReason` SHALL be set to a human-readable message including the error type (e.g. `"Auto-disabled after 5 consecutive 404 errors"`). Transient failures SHALL NOT count toward the auto-disable threshold — only permanent failures trigger auto-disable.

#### Scenario: Source disabled after 5 permanent failures
- **WHEN** a source reaches 5 consecutive permanent failures (e.g. HTTP 404)
- **THEN** the source is set to `enabled = false` with `disabledReason = "Auto-disabled after 5 consecutive 404 errors"`

#### Scenario: Transient failures do not trigger auto-disable
- **WHEN** a source has 10 consecutive transient failures (e.g. timeouts)
- **THEN** the source remains `enabled = true` (only permanent failures count toward auto-disable)

#### Scenario: Mixed failures — permanent counter resets on transient
- **WHEN** a source has 3 consecutive permanent failures followed by a transient failure
- **THEN** the permanent failure count is not considered reset — `consecutiveFailures` continues to increment, but auto-disable only triggers when the last N failures are ALL permanent

### Requirement: Reset failure state on re-enable
The system SHALL reset all failure tracking fields when a source is re-enabled via the API. Specifically, when `enabled` is set from `false` to `true`, the system SHALL set `consecutiveFailures` to 0, `lastFailureType` to null, and `disabledReason` to null.

#### Scenario: User re-enables a disabled source
- **WHEN** a user updates a source with `enabled = false` and `disabledReason = "Auto-disabled after 5 consecutive 404 errors"` to `enabled = true`
- **THEN** `consecutiveFailures` is set to 0, `lastFailureType` is set to null, and `disabledReason` is set to null

### Requirement: Configurable failure thresholds
The system SHALL support the following configuration properties:
- `app.source.max-failures` (default: 5) — number of consecutive permanent failures before auto-disable
- `app.source.max-backoff-hours` (default: 24) — maximum backoff interval cap in hours

#### Scenario: Custom max-failures threshold
- **WHEN** `app.source.max-failures` is set to 3 and a source reaches 3 consecutive permanent failures
- **THEN** the source is auto-disabled

#### Scenario: Custom max-backoff-hours
- **WHEN** `app.source.max-backoff-hours` is set to 6 and a source has `consecutiveFailures = 10` with `pollIntervalMinutes = 60`
- **THEN** the backoff is capped at 360 minutes (6 hours), not 24 hours

## MODIFIED Requirements

### Requirement: Auto-disable after permanent failures
The system SHALL auto-disable a source after `app.source.max-failures` (default: 15) consecutive **permanent** failures. When the threshold is reached, the source's `enabled` field SHALL be set to `false` and `disabledReason` SHALL be set to a human-readable message including the error type (e.g. `"Auto-disabled after 15 consecutive 404 errors"`). Transient failures SHALL NOT count toward the auto-disable threshold — only permanent failures trigger auto-disable.

#### Scenario: Source disabled after 15 permanent failures
- **WHEN** a source reaches 15 consecutive permanent failures (e.g. HTTP 404)
- **THEN** the source is set to `enabled = false` with `disabledReason = "Auto-disabled after 15 consecutive 404 errors"`

#### Scenario: Transient failures do not trigger auto-disable
- **WHEN** a source has 20 consecutive transient failures (e.g. timeouts)
- **THEN** the source remains `enabled = true` (only permanent failures count toward auto-disable)

#### Scenario: Mixed failures — permanent counter resets on transient
- **WHEN** a source has 3 consecutive permanent failures followed by a transient failure
- **THEN** the permanent failure count is not considered reset — `consecutiveFailures` continues to increment, but auto-disable only triggers when the last N failures are ALL permanent

### Requirement: Configurable failure thresholds
The system SHALL support the following configuration properties:
- `app.source.max-failures` (default: 15) — number of consecutive permanent failures before auto-disable
- `app.source.max-backoff-hours` (default: 24) — maximum backoff interval cap in hours

#### Scenario: Custom max-failures threshold
- **WHEN** `app.source.max-failures` is set to 3 and a source reaches 3 consecutive permanent failures
- **THEN** the source is auto-disabled

#### Scenario: Custom max-backoff-hours
- **WHEN** `app.source.max-backoff-hours` is set to 6 and a source has `consecutiveFailures = 10` with `pollIntervalMinutes = 60`
- **THEN** the backoff is capped at 360 minutes (6 hours), not 24 hours

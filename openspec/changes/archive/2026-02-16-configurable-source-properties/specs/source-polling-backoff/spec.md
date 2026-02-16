## MODIFIED Requirements

### Requirement: Configurable failure thresholds
The system SHALL support the following configuration properties as global defaults:
- `app.source.max-failures` (default: 15) — number of consecutive permanent failures before auto-disable
- `app.source.max-backoff-hours` (default: 24) — maximum backoff interval cap in hours

Each source MAY override these defaults via nullable `maxFailures` and `maxBackoffHours` fields on the `Source` entity. The effective value SHALL be `source.maxFailures ?: appProperties.source.maxFailures` and `source.maxBackoffHours ?: appProperties.source.maxBackoffHours`.

#### Scenario: Source with custom max-failures
- **WHEN** a source has `maxFailures` set to 3 and reaches 3 consecutive permanent failures
- **THEN** the source is auto-disabled (using the per-source override, not the global default)

#### Scenario: Source without override uses global
- **WHEN** a source has `maxFailures` set to null and reaches the global default (15) consecutive permanent failures
- **THEN** the source is auto-disabled

#### Scenario: Source with custom max-backoff-hours
- **WHEN** a source has `maxBackoffHours` set to 48 and `consecutiveFailures = 10` with `pollIntervalMinutes = 60`
- **THEN** the backoff is capped at 2880 minutes (48 hours)

#### Scenario: Source without backoff override uses global
- **WHEN** a source has `maxBackoffHours` set to null and `consecutiveFailures = 10` with `pollIntervalMinutes = 60`
- **THEN** the backoff is capped at 1440 minutes (24 hours, the global default)

### Requirement: Auto-disable after permanent failures
The system SHALL auto-disable a source after the effective `maxFailures` threshold is reached with consecutive **permanent** failures. The effective threshold SHALL be `source.maxFailures ?: appProperties.source.maxFailures`. When the threshold is reached, the source's `enabled` field SHALL be set to `false` and `disabledReason` SHALL be set to a human-readable message including the error type.

### Requirement: Exponential backoff for failed sources
The system SHALL apply exponential backoff when deciding when to next poll a source that has consecutive failures. The backoff delay SHALL be computed as `pollIntervalMinutes × 2^consecutiveFailures`, capped at the effective `maxBackoffHours` (source override or global default) converted to minutes.

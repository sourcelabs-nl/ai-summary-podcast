## MODIFIED Requirements

### Requirement: Source configuration via YAML
The system SHALL load content source definitions from a `sources.yml` file using Spring's `@ConfigurationProperties`. Each source entry SHALL have the fields: `id` (unique string), `type` (one of `rss`, `website`, `twitter`), `url`, `pollIntervalMinutes` (default: 60), `enabled` (default: true), and `aggregate` (nullable boolean, default: null — enabling hybrid auto-detect/override for article aggregation).

#### Scenario: Valid source configuration loaded at startup
- **WHEN** the application starts with a valid `sources.yml` containing two RSS sources and one website source
- **THEN** all three sources are available as `SourceProperties` beans with their configured values

#### Scenario: Source with default values
- **WHEN** a source entry omits `pollIntervalMinutes`, `enabled`, and `aggregate`
- **THEN** the source defaults to a 60-minute poll interval, enabled = true, and aggregate = null (auto-detect)

#### Scenario: Disabled source is loaded but not active
- **WHEN** a source entry has `enabled: false`
- **THEN** the source is loaded into configuration but excluded from polling

#### Scenario: Source with explicit aggregate override
- **WHEN** a source entry has `aggregate: true`
- **THEN** articles from this source are always aggregated regardless of type or URL

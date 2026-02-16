## MODIFIED Requirements

### Requirement: Source configuration via YAML
The system SHALL load content source definitions from a `sources.yml` file using Spring's `@ConfigurationProperties`. Each source entry SHALL have the fields: `id` (unique string), `type` (one of `rss`, `website`, `twitter`), `url`, `pollIntervalMinutes` (default: 60), `enabled` (default: true), and `aggregate` (nullable boolean, default: null — enabling hybrid auto-detect/override for article aggregation).

Each source SHALL additionally have optional `maxFailures` (nullable INTEGER) and `maxBackoffHours` (nullable INTEGER) fields. When set, these override the global `app.source.max-failures` and `app.source.max-backoff-hours` defaults for that specific source. When null, the global defaults apply.

The `maxFailures` and `maxBackoffHours` fields SHALL be accepted as optional nullable integers in the source create (`POST`) and update (`PUT`) endpoints. They SHALL be included in the source GET response.

#### Scenario: Create source with custom failure threshold
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/sources` request includes `"maxFailures": 20`
- **THEN** the source is created with `max_failures` set to 20

#### Scenario: Create source without failure threshold
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/sources` request does not include `maxFailures`
- **THEN** the source is created with `max_failures` as null (uses global default)

#### Scenario: Update source backoff hours
- **WHEN** a `PUT /users/{userId}/podcasts/{podcastId}/sources/{sourceId}` request includes `"maxBackoffHours": 48`
- **THEN** the source's `max_backoff_hours` is updated to 48

#### Scenario: Get source includes failure config
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}/sources` response includes a source with `max_failures` set to 20 and `max_backoff_hours` set to 48
- **THEN** the response includes `"maxFailures": 20` and `"maxBackoffHours": 48`

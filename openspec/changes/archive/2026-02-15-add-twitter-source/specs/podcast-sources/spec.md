## MODIFIED Requirements

### Requirement: Add source for a podcast
The system SHALL allow adding a content source scoped to a specific podcast. Each source record SHALL have: `id` (TEXT, primary key), `podcast_id` (TEXT, FK to podcasts), `type` (TEXT, one of: rss, website, twitter), `url` (TEXT), `poll_interval_minutes` (INTEGER, default 60), `enabled` (INTEGER, default 1), `last_polled` (TEXT, nullable), `last_seen_id` (TEXT, nullable).

#### Scenario: Add an RSS source to a podcast
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/sources` request is received with `type: "rss"`, `url`, and optionally `pollIntervalMinutes` and `enabled`
- **THEN** the system creates a source record linked to the podcast and returns HTTP 201 with the created source

#### Scenario: Add a Twitter source to a podcast
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/sources` request is received with `type: "twitter"` and `url` set to an X username or full X URL
- **THEN** the system creates a source record linked to the podcast and returns HTTP 201 with the created source

#### Scenario: Add source for non-existing podcast
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/sources` request is received for a podcast that does not exist
- **THEN** the system returns HTTP 404

#### Scenario: Add source with missing required fields
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/sources` request is received without `type` or `url`
- **THEN** the system returns HTTP 400 with a validation error message

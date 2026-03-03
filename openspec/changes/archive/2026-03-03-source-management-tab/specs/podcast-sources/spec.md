## MODIFIED Requirements

### Requirement: Add source for a podcast
The system SHALL allow adding a content source scoped to a specific podcast. Each source record SHALL have: `id` (TEXT, primary key), `podcast_id` (TEXT, FK to podcasts), `type` (TEXT, one of: rss, website, twitter), `url` (TEXT), `poll_interval_minutes` (INTEGER, default 60), `enabled` (INTEGER, default 1), `last_polled` (TEXT, nullable), `last_seen_id` (TEXT, nullable). The create request SHALL accept an optional `label` (string, nullable) field for a human-readable display name.

#### Scenario: Add an RSS source to a podcast
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/sources` request is received with `type: "rss"`, `url`, and optionally `pollIntervalMinutes`, `enabled`, and `label`
- **THEN** the system creates a source record linked to the podcast (with label if provided) and returns HTTP 201 with the created source

#### Scenario: Add a Twitter source to a podcast
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/sources` request is received with `type: "twitter"` and `url` set to an X username or full X URL
- **THEN** the system creates a source record linked to the podcast and returns HTTP 201 with the created source

#### Scenario: Add source for non-existing podcast
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/sources` request is received for a podcast that does not exist
- **THEN** the system returns HTTP 404

#### Scenario: Add source with missing required fields
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/sources` request is received without `type` or `url`
- **THEN** the system returns HTTP 400 with a validation error message

### Requirement: Update a podcast's source
The system SHALL allow updating a source's `url`, `type`, `pollIntervalMinutes`, `enabled`, and `label` fields.

#### Scenario: Update source configuration
- **WHEN** a `PUT /users/{userId}/podcasts/{podcastId}/sources/{sourceId}` request is received with updated fields (including optional `label`)
- **THEN** the system updates the source record and returns HTTP 200 with the updated source

#### Scenario: Update source belonging to a different podcast
- **WHEN** a `PUT /users/{userId}/podcasts/{podcastId}/sources/{sourceId}` request is received but the source belongs to a different podcast
- **THEN** the system returns HTTP 404

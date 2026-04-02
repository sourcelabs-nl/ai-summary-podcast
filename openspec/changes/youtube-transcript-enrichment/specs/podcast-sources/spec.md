## MODIFIED Requirements

### Requirement: Add source for a podcast
The system SHALL allow adding a content source scoped to a specific podcast. Each source record SHALL have: `id` (TEXT, primary key), `podcast_id` (TEXT, FK to podcasts), `type` (TEXT, one of: rss, website, twitter, youtube), `url` (TEXT), `poll_interval_minutes` (INTEGER, default 60), `enabled` (INTEGER, default 1), `last_polled` (TEXT, nullable), `last_seen_id` (TEXT, nullable). The create request SHALL accept an optional `label` (string, nullable) field for a human-readable display name.

For YouTube sources, the system SHALL accept channel URLs in user-friendly formats (`https://www.youtube.com/@ChannelName`, `https://www.youtube.com/@ChannelName/videos`, `https://www.youtube.com/channel/CHANNEL_ID`) and resolve them to the RSS feed URL (`https://www.youtube.com/feeds/videos.xml?channel_id=CHANNEL_ID`). The resolved RSS URL SHALL be stored as the source URL. If the channel URL cannot be resolved, the system SHALL return HTTP 422 with an error message. Direct RSS feed URLs (`youtube.com/feeds/videos.xml?channel_id=...`) SHALL be accepted without resolution.

#### Scenario: Add an RSS source to a podcast
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/sources` request is received with `type: "rss"`, `url`, and optionally `pollIntervalMinutes`, `enabled`, and `label`
- **THEN** the system creates a source record linked to the podcast (with label if provided) and returns HTTP 201 with the created source

#### Scenario: Add a Twitter source to a podcast
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/sources` request is received with `type: "twitter"` and `url` set to an X username or full X URL
- **THEN** the system creates a source record linked to the podcast and returns HTTP 201 with the created source

#### Scenario: Add a YouTube source with channel URL
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/sources` request is received with `type: "youtube"` and `url: "https://www.youtube.com/@Fireship"`
- **THEN** the system resolves the channel ID, stores the source with the RSS feed URL (`https://www.youtube.com/feeds/videos.xml?channel_id=...`), and returns HTTP 201

#### Scenario: Add a YouTube source with direct RSS URL
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/sources` request is received with `type: "youtube"` and `url: "https://www.youtube.com/feeds/videos.xml?channel_id=UC123"`
- **THEN** the system creates the source with the URL as-is and returns HTTP 201

#### Scenario: Add a YouTube source with invalid channel URL
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/sources` request is received with `type: "youtube"` and a URL that cannot be resolved to a channel ID
- **THEN** the system returns HTTP 422 with an error message explaining the URL could not be resolved

#### Scenario: Add source for non-existing podcast
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/sources` request is received for a podcast that does not exist
- **THEN** the system returns HTTP 404

#### Scenario: Add source with missing required fields
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/sources` request is received without `type` or `url`
- **THEN** the system returns HTTP 400 with a validation error message

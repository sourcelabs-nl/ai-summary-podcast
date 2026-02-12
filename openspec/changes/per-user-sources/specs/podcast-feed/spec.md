## ADDED Requirements

### Requirement: Podcast-scoped RSS feed
The system SHALL serve an RSS 2.0 feed per podcast at `/users/{userId}/podcasts/{podcastId}/feed.xml`. The feed SHALL contain only episodes belonging to that podcast.

#### Scenario: Get feed for podcast with episodes
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}/feed.xml` request is received for a podcast with 5 episodes
- **THEN** the system returns HTTP 200 with Content-Type `application/rss+xml` containing an RSS 2.0 feed with 5 entries, sorted by most recent first

#### Scenario: Get feed for podcast with no episodes
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}/feed.xml` request is received for a podcast with no episodes
- **THEN** the system returns HTTP 200 with a valid RSS 2.0 feed containing zero entries

#### Scenario: Get feed for non-existing podcast
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}/feed.xml` request is received for a podcast that does not exist
- **THEN** the system returns HTTP 404

### Requirement: Feed metadata includes podcast and user name
The RSS feed title SHALL incorporate the podcast name and user name to distinguish feeds in podcast apps (e.g., "{configured feed title} - {userName} - {podcastName}").

#### Scenario: Feed title with user and podcast name
- **WHEN** the RSS feed is generated for user "Jeroen" and podcast "AI News"
- **THEN** the feed title is "{configured feed title} - Jeroen - AI News"

### Requirement: Episode audio served from podcast-scoped paths
Episode audio files SHALL be accessible via `/episodes/{podcastId}/{filename}` and the RSS feed `<enclosure>` URLs SHALL reference this path.

#### Scenario: Audio file URL in feed
- **WHEN** the feed is generated for a podcast with an episode whose audio file is at `data/episodes/{podcastId}/briefing-20260101-060000.mp3`
- **THEN** the enclosure URL in the feed entry is `{baseUrl}/episodes/{podcastId}/briefing-20260101-060000.mp3`

#### Scenario: Serve audio file
- **WHEN** a `GET /episodes/{podcastId}/{filename}` request is received for an existing audio file
- **THEN** the system returns the MP3 file with Content-Type `audio/mpeg`

### Requirement: Episode cleanup scoped to podcast
Episode cleanup (retention policy) SHALL operate per-podcast, deleting expired episodes and their audio files from the podcast-scoped directory.

#### Scenario: Clean up old episodes
- **WHEN** the cleanup job runs and a podcast has episodes older than the retention period
- **THEN** those episodes are deleted from the database and their audio files are removed from the podcast's episode directory

### Requirement: Remove global feed endpoint
The global `/feed.xml` endpoint SHALL be removed. The global `/generate` endpoint SHALL be removed. These are replaced by podcast-scoped equivalents.

#### Scenario: Access old global feed
- **WHEN** a `GET /feed.xml` request is received
- **THEN** the system returns HTTP 404 (or does not match any route)

#### Scenario: Access old global generate
- **WHEN** a `POST /generate` request is received
- **THEN** the system returns HTTP 404 (or does not match any route)

## MODIFIED Requirements

### Requirement: Discard episode script
The system SHALL allow discarding an episode script that is in `PENDING_REVIEW` status. When an episode is discarded, the system SHALL reset all articles that contributed to the episode by setting their `is_processed` flag to `false`, making them available for future episode generation.

The system SHALL look up linked articles via the `episode_articles` table. If no episode-article links exist (for episodes created before the tracking feature was added), the system SHALL log a warning indicating that no articles could be reset.

#### Scenario: Discard pending episode with article links
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/discard` request is received, the episode status is `PENDING_REVIEW`, and the episode has 5 linked articles in `episode_articles`
- **THEN** the system updates the episode status to `DISCARDED`, resets all 5 articles to `is_processed = false`, and returns HTTP 200

#### Scenario: Discard pending episode without article links
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/discard` request is received, the episode status is `PENDING_REVIEW`, and the episode has no linked articles in `episode_articles`
- **THEN** the system updates the episode status to `DISCARDED`, logs a warning that no articles could be reset, and returns HTTP 200

#### Scenario: Discard non-pending episode
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/discard` request is received and the episode status is not `PENDING_REVIEW`
- **THEN** the system returns HTTP 409 (Conflict) with an error message

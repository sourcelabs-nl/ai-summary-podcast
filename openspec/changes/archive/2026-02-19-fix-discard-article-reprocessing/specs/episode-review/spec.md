## MODIFIED Requirements

### Requirement: Discard episode script
The system SHALL allow discarding an episode script that is in `PENDING_REVIEW` status. When an episode is discarded, the system SHALL reset the `isProcessed` flag to `false` on all articles linked to that episode via the `episode_articles` table, making them eligible for inclusion in future episode generation.

#### Scenario: Discard pending episode
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/discard` request is received and the episode status is `PENDING_REVIEW`
- **THEN** the system updates the episode status to `DISCARDED`, resets `isProcessed` to `false` on all articles linked to the episode, and returns HTTP 200

#### Scenario: Discard non-pending episode
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/discard` request is received and the episode status is not `PENDING_REVIEW`
- **THEN** the system returns HTTP 409 (Conflict) with an error message

#### Scenario: Discarded episode articles appear in next generation
- **WHEN** an episode has been discarded and the pipeline runs again for the same podcast
- **THEN** the articles from the discarded episode (that still meet the relevance threshold) SHALL be included in the new episode's composition

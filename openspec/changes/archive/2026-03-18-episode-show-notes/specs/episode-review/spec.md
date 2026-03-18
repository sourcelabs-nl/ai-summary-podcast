## MODIFIED Requirements

### Requirement: Get single episode
The system SHALL provide an endpoint to retrieve a single episode by ID, including its script text and show notes.

#### Scenario: Get existing episode
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}` request is received for an existing episode belonging to the podcast
- **THEN** the system returns HTTP 200 with the episode details including `id`, `status`, `scriptText`, `showNotes`, `audioFilePath`, `durationSeconds`, and `generatedAt`

## MODIFIED Requirements

### Requirement: Episode status lifecycle
Each episode SHALL have a `status` field with one of the following values: `PENDING_REVIEW`, `APPROVED`, `GENERATED`, `FAILED`, `DISCARDED`. The status determines where the episode is in the review-to-audio pipeline. After each status transition, the service SHALL publish a `PodcastEvent` via `ApplicationEventPublisher` to notify connected clients.

#### Scenario: New episode created with review enabled
- **WHEN** the pipeline generates a script for a podcast with `requireReview = true`
- **THEN** an episode is created with status `PENDING_REVIEW`, the `scriptText` populated, `audioFilePath` and `durationSeconds` set to null, and an `episode.created` event is published

#### Scenario: New episode created without review
- **WHEN** the pipeline generates a script for a podcast with `requireReview = false`
- **THEN** the episode is created with status `GENERATED` after TTS completes, with all fields populated, and an `episode.generated` event is published

### Requirement: Approve episode script
The system SHALL allow approving an episode script, which triggers async TTS generation. Each status transition during the approval and generation flow SHALL publish a corresponding event.

#### Scenario: Approve pending episode
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/approve` request is received and the episode status is `PENDING_REVIEW`
- **THEN** the system updates the episode status to `APPROVED`, publishes an `episode.approved` event, triggers TTS generation asynchronously, and returns HTTP 202 (Accepted)

#### Scenario: Approve non-pending episode
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/approve` request is received and the episode status is not `PENDING_REVIEW`
- **THEN** the system returns HTTP 409 (Conflict) with an error message

#### Scenario: TTS generation starts
- **WHEN** the async TTS pipeline begins generating audio for an approved episode
- **THEN** an `episode.audio.started` event is published

#### Scenario: TTS generation succeeds after approval
- **WHEN** the async TTS pipeline completes successfully for an approved episode
- **THEN** the episode status is updated to `GENERATED`, `audioFilePath` and `durationSeconds` are populated, and an `episode.generated` event is published

#### Scenario: TTS generation fails after approval
- **WHEN** the async TTS pipeline fails for an approved episode
- **THEN** the episode status is updated to `FAILED` and an `episode.failed` event is published

### Requirement: Retry failed episode
The system SHALL allow re-triggering TTS for a `FAILED` episode by approving it again.

#### Scenario: Approve failed episode
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/approve` request is received and the episode status is `FAILED`
- **THEN** the system updates the episode status to `APPROVED`, publishes an `episode.approved` event, triggers TTS generation asynchronously, and returns HTTP 202

### Requirement: Discard episode script
The system SHALL allow discarding an episode script that is in `PENDING_REVIEW` status. When an episode is discarded, the service SHALL publish an `episode.discarded` event. Article handling behavior (non-aggregated reset, aggregated deletion) remains unchanged.

#### Scenario: Discard pending episode
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/discard` request is received and the episode status is `PENDING_REVIEW`
- **THEN** the system updates the episode status to `DISCARDED`, handles linked articles as before, publishes an `episode.discarded` event, and returns HTTP 200

#### Scenario: Discard non-pending episode
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/discard` request is received and the episode status is not `PENDING_REVIEW`
- **THEN** the system returns HTTP 409 (Conflict) with an error message

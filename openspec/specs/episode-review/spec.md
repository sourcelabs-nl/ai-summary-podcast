# Capability: Episode Review

## Purpose

Review workflow for episode scripts — status lifecycle, script editing, approval/discard, and async TTS trigger after approval.

## Requirements

### Requirement: Episode status lifecycle
Each episode SHALL have a `status` field with one of the following values: `PENDING_REVIEW`, `APPROVED`, `GENERATED`, `FAILED`, `DISCARDED`. The status determines where the episode is in the review-to-audio pipeline.

#### Scenario: New episode created with review enabled
- **WHEN** the pipeline generates a script for a podcast with `requireReview = true`
- **THEN** an episode is created with status `PENDING_REVIEW`, the `scriptText` populated, and `audioFilePath` and `durationSeconds` set to null

#### Scenario: New episode created without review
- **WHEN** the pipeline generates a script for a podcast with `requireReview = false`
- **THEN** the episode is created with status `GENERATED` after TTS completes, with all fields populated (current behavior)

### Requirement: List episodes for a podcast
The system SHALL provide an endpoint to list episodes for a podcast, with optional status filtering.

#### Scenario: List all episodes
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}/episodes` request is received
- **THEN** the system returns HTTP 200 with a JSON array of all episodes for that podcast, ordered by `generatedAt` descending

#### Scenario: List episodes filtered by status
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}/episodes?status=PENDING_REVIEW` request is received
- **THEN** the system returns HTTP 200 with only episodes matching the given status

#### Scenario: List episodes for non-existing podcast
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}/episodes` request is received for a podcast that does not exist or belongs to a different user
- **THEN** the system returns HTTP 404

### Requirement: Get single episode
The system SHALL provide an endpoint to retrieve a single episode by ID, including its script text.

#### Scenario: Get existing episode
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}` request is received for an existing episode belonging to the podcast
- **THEN** the system returns HTTP 200 with the episode details including `id`, `status`, `scriptText`, `audioFilePath`, `durationSeconds`, and `generatedAt`

#### Scenario: Get non-existing episode
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}` request is received for an episode that does not exist or belongs to a different podcast
- **THEN** the system returns HTTP 404

### Requirement: Edit episode script
The system SHALL allow editing the script text of an episode that is in `PENDING_REVIEW` status.

#### Scenario: Edit script of pending episode
- **WHEN** a `PUT /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/script` request is received with a JSON body containing `scriptText`, and the episode status is `PENDING_REVIEW`
- **THEN** the system updates the episode's `scriptText` and returns HTTP 200 with the updated episode

#### Scenario: Edit script of non-pending episode
- **WHEN** a `PUT /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/script` request is received and the episode status is not `PENDING_REVIEW`
- **THEN** the system returns HTTP 409 (Conflict) with an error message indicating the episode is not in a reviewable state

### Requirement: Approve episode script
The system SHALL allow approving an episode script, which triggers async TTS generation.

#### Scenario: Approve pending episode
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/approve` request is received and the episode status is `PENDING_REVIEW`
- **THEN** the system updates the episode status to `APPROVED`, triggers TTS generation asynchronously, and returns HTTP 202 (Accepted)

#### Scenario: Approve non-pending episode
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/approve` request is received and the episode status is not `PENDING_REVIEW`
- **THEN** the system returns HTTP 409 (Conflict) with an error message

#### Scenario: TTS generation succeeds after approval
- **WHEN** the async TTS pipeline completes successfully for an approved episode
- **THEN** the episode status is updated to `GENERATED` and `audioFilePath` and `durationSeconds` are populated

#### Scenario: TTS generation fails after approval
- **WHEN** the async TTS pipeline fails for an approved episode
- **THEN** the episode status is updated to `FAILED`

### Requirement: Retry failed episode
The system SHALL allow re-triggering TTS for a `FAILED` episode by approving it again.

#### Scenario: Approve failed episode
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/approve` request is received and the episode status is `FAILED`
- **THEN** the system updates the episode status to `APPROVED`, triggers TTS generation asynchronously, and returns HTTP 202

### Requirement: Discard episode script
The system SHALL allow discarding an episode script that is in `PENDING_REVIEW` status.

#### Scenario: Discard pending episode
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/discard` request is received and the episode status is `PENDING_REVIEW`
- **THEN** the system updates the episode status to `DISCARDED` and returns HTTP 200

#### Scenario: Discard non-pending episode
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/discard` request is received and the episode status is not `PENDING_REVIEW`
- **THEN** the system returns HTTP 409 (Conflict) with an error message

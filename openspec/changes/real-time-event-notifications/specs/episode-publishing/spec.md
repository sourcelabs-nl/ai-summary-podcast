## MODIFIED Requirements

### Requirement: Publish episode to target endpoint
The system SHALL provide a `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/publish/{target}` endpoint. The endpoint SHALL look up the publisher by target name, verify the episode exists with status `GENERATED` and has an audio file, then delegate to the publisher. After the publish operation completes (success or failure), the service SHALL publish a `PodcastEvent` to notify connected clients. On success, it SHALL return HTTP 200 with the publication record. On failure, it SHALL return an appropriate error.

#### Scenario: Successful publish
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/publish/soundcloud` request is received for a GENERATED episode with an audio file and the user has a connected SoundCloud account
- **THEN** the system uploads the episode to SoundCloud, creates an `episode_publications` record with status `PUBLISHED`, publishes an `episode.published` event, and returns HTTP 200 with the publication details

#### Scenario: Publish failure
- **WHEN** the publish operation fails for any reason (network error, API error)
- **THEN** the system updates the publication record to status `FAILED` with `error_message`, publishes an `episode.publish.failed` event, and returns an error response

#### Scenario: Episode not in GENERATED status
- **WHEN** a `POST .../publish/soundcloud` request is received for an episode with status `PENDING_REVIEW`
- **THEN** the system returns HTTP 400 indicating the episode must be in GENERATED status

#### Scenario: Episode has no audio file
- **WHEN** a `POST .../publish/soundcloud` request is received for a GENERATED episode where `audioFilePath` is null
- **THEN** the system returns HTTP 400 indicating the episode has no audio file

#### Scenario: Unknown publish target
- **WHEN** a `POST .../publish/youtube` request is received and no publisher is registered for `"youtube"`
- **THEN** the system returns HTTP 400 indicating the target is not supported

#### Scenario: Episode already published to target
- **WHEN** a `POST .../publish/soundcloud` request is received for an episode that already has a `PUBLISHED` record for `"soundcloud"`
- **THEN** the system returns HTTP 409 indicating the episode is already published to this target

#### Scenario: Episode not found
- **WHEN** a `POST .../publish/soundcloud` request is received for an episode ID that does not exist or does not belong to the specified podcast
- **THEN** the system returns HTTP 404

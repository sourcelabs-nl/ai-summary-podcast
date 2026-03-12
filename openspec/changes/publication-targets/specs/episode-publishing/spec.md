## MODIFIED Requirements

### Requirement: Publish episode to target endpoint
The system SHALL provide a `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/publish/{target}` endpoint. The endpoint SHALL look up the publisher by target name, verify the episode exists with status `GENERATED` and has an audio file, verify that the podcast has an **enabled** `podcast_publication_targets` entry for the requested target, then delegate to the publisher. On success, it SHALL return HTTP 200 with the publication record. On failure, it SHALL return an appropriate error.

#### Scenario: Successful publish
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/publish/soundcloud` request is received for a GENERATED episode with an audio file, the user has a connected SoundCloud account, and the podcast has an enabled SoundCloud publication target
- **THEN** the system uploads the episode to SoundCloud, creates an `episode_publications` record with status `PUBLISHED`, and returns HTTP 200 with the publication details (external ID, external URL)

#### Scenario: Episode not in GENERATED status
- **WHEN** a `POST .../publish/soundcloud` request is received for an episode with status `PENDING_REVIEW`
- **THEN** the system returns HTTP 400 indicating the episode must be in GENERATED status

#### Scenario: Episode has no audio file
- **WHEN** a `POST .../publish/soundcloud` request is received for a GENERATED episode where `audioFilePath` is null
- **THEN** the system returns HTTP 400 indicating the episode has no audio file

#### Scenario: Unknown publish target
- **WHEN** a `POST .../publish/youtube` request is received and no publisher is registered for `"youtube"`
- **THEN** the system returns HTTP 400 indicating the target is not supported

#### Scenario: Target not enabled on podcast
- **WHEN** a `POST .../publish/ftp` request is received but the podcast has no enabled `podcast_publication_targets` entry for `"ftp"`
- **THEN** the system returns HTTP 400 indicating the publication target is not configured or not enabled for this podcast

#### Scenario: Episode already published to target
- **WHEN** a `POST .../publish/soundcloud` request is received for an episode that already has a `PUBLISHED` record for `"soundcloud"`
- **THEN** the system returns HTTP 409 indicating the episode is already published to this target

#### Scenario: Episode not found
- **WHEN** a `POST .../publish/soundcloud` request is received for an episode ID that does not exist or does not belong to the specified podcast
- **THEN** the system returns HTTP 404

## REMOVED Requirements

### Requirement: SoundCloud playlist ID storage on podcast
**Reason**: Replaced by generic `podcast_publication_targets` table. The playlist ID is now stored as JSON config (`{"playlistId": "..."}`) in the `podcast_publication_targets` row for target `"soundcloud"`.
**Migration**: Flyway migration copies existing `soundcloud_playlist_id` values into `podcast_publication_targets` before dropping the column.

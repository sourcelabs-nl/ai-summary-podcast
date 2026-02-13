# Capability: Episode Publishing

## Purpose

Publish generated podcast episodes to external platforms (e.g., SoundCloud) via a pluggable publisher abstraction with publication status tracking.

## Requirements

### Requirement: Publisher interface and registry
The system SHALL define an `EpisodePublisher` interface with a `publish(episode, podcast, userId)` method that uploads an episode to an external platform and returns a `PublishResult` containing the external ID and URL. Each implementation SHALL be a Spring bean identified by a target name string (e.g., `"soundcloud"`). A `PublisherRegistry` Spring component SHALL collect all `EpisodePublisher` beans and provide a `getPublisher(target): EpisodePublisher?` lookup method.

#### Scenario: Registry resolves known target
- **WHEN** `PublisherRegistry.getPublisher("soundcloud")` is called and a `SoundCloudPublisher` bean exists
- **THEN** the registry returns the `SoundCloudPublisher` instance

#### Scenario: Registry returns null for unknown target
- **WHEN** `PublisherRegistry.getPublisher("youtube")` is called and no publisher is registered for `"youtube"`
- **THEN** the registry returns null

### Requirement: Publish episode to target endpoint
The system SHALL provide a `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/publish/{target}` endpoint. The endpoint SHALL look up the publisher by target name, verify the episode exists with status `GENERATED` and has an audio file, then delegate to the publisher. On success, it SHALL return HTTP 200 with the publication record. On failure, it SHALL return an appropriate error.

#### Scenario: Successful publish
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/publish/soundcloud` request is received for a GENERATED episode with an audio file and the user has a connected SoundCloud account
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

#### Scenario: Episode already published to target
- **WHEN** a `POST .../publish/soundcloud` request is received for an episode that already has a `PUBLISHED` record for `"soundcloud"`
- **THEN** the system returns HTTP 409 indicating the episode is already published to this target

#### Scenario: Episode not found
- **WHEN** a `POST .../publish/soundcloud` request is received for an episode ID that does not exist or does not belong to the specified podcast
- **THEN** the system returns HTTP 404

### Requirement: Publication status tracking
The system SHALL store publication records in an `episode_publications` table with columns: `id` (INTEGER, auto-increment PK), `episode_id` (INTEGER, FK to episodes), `target` (TEXT, NOT NULL), `status` (TEXT, NOT NULL — PENDING, PUBLISHED, FAILED), `external_id` (TEXT, nullable), `external_url` (TEXT, nullable), `error_message` (TEXT, nullable), `published_at` (TEXT, nullable — ISO-8601), `created_at` (TEXT, NOT NULL — ISO-8601). A unique constraint SHALL exist on `(episode_id, target)`.

#### Scenario: Publication record created on publish attempt
- **WHEN** a user triggers publishing an episode to SoundCloud
- **THEN** a record is created in `episode_publications` with status `PENDING` and the `created_at` timestamp set

#### Scenario: Publication record updated on success
- **WHEN** the SoundCloud upload completes successfully
- **THEN** the record is updated to status `PUBLISHED` with `external_id`, `external_url`, and `published_at` set

#### Scenario: Publication record updated on failure
- **WHEN** the SoundCloud upload fails
- **THEN** the record is updated to status `FAILED` with `error_message` containing the failure reason

### Requirement: List episode publications endpoint
The system SHALL provide a `GET /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/publications` endpoint that returns all publication records for the specified episode as a JSON array.

#### Scenario: Episode with publications
- **WHEN** a `GET .../publications` request is received for an episode published to SoundCloud
- **THEN** the system returns HTTP 200 with a JSON array containing the publication record (target, status, external_id, external_url, published_at)

#### Scenario: Episode with no publications
- **WHEN** a `GET .../publications` request is received for an episode that has not been published anywhere
- **THEN** the system returns HTTP 200 with an empty JSON array

#### Scenario: Episode not found
- **WHEN** a `GET .../publications` request is received for an episode that does not exist
- **THEN** the system returns HTTP 404

### Requirement: SoundCloud playlist ID storage on podcast
The `podcasts` table SHALL have a nullable `soundcloud_playlist_id` column (TEXT) that stores the SoundCloud playlist ID associated with the podcast. The `Podcast` entity SHALL include a `soundcloudPlaylistId` field. A database migration SHALL add this column.

#### Scenario: New podcast has no playlist ID
- **WHEN** a new podcast is created
- **THEN** the `soundcloud_playlist_id` column is null

#### Scenario: Playlist ID stored after first SoundCloud publish
- **WHEN** the first episode of a podcast is published to SoundCloud and a playlist is created
- **THEN** the `soundcloud_playlist_id` column is updated with the SoundCloud playlist ID

#### Scenario: Playlist ID updated when playlist is recreated
- **WHEN** a publish detects a stale playlist (404) and creates a new one
- **THEN** the `soundcloud_playlist_id` column is updated with the new playlist ID

### Requirement: Cascade delete publications with episode
When an episode is deleted (e.g., during cleanup), all of its publication records SHALL be deleted as part of the cascade.

#### Scenario: Episode cleanup removes publications
- **WHEN** an episode with two publication records is deleted by the cleanup scheduler
- **THEN** both publication records are removed from the `episode_publications` table

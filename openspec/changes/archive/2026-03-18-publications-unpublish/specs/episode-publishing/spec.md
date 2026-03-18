## MODIFIED Requirements

### Requirement: Publication status tracking
The system SHALL store publication records in an `episode_publications` table with columns: `id` (INTEGER, auto-increment PK), `episode_id` (INTEGER, FK to episodes), `target` (TEXT, NOT NULL), `status` (TEXT, NOT NULL — PENDING, PUBLISHED, FAILED, UNPUBLISHED), `external_id` (TEXT, nullable), `external_url` (TEXT, nullable), `error_message` (TEXT, nullable), `published_at` (TEXT, nullable — ISO-8601), `created_at` (TEXT, NOT NULL — ISO-8601). A unique constraint SHALL exist on `(episode_id, target)`.

#### Scenario: Publication record created on publish attempt
- **WHEN** a user triggers publishing an episode to SoundCloud
- **THEN** a record is created in `episode_publications` with status `PENDING` and the `created_at` timestamp set

#### Scenario: Publication record updated on success
- **WHEN** the SoundCloud upload completes successfully
- **THEN** the record is updated to status `PUBLISHED` with `external_id`, `external_url`, and `published_at` set

#### Scenario: Publication record updated on failure
- **WHEN** the SoundCloud upload fails
- **THEN** the record is updated to status `FAILED` with `error_message` containing the failure reason

#### Scenario: Publication record updated on unpublish
- **WHEN** a user unpublishes an episode from a target
- **THEN** the record is updated to status `UNPUBLISHED` and `external_id` is cleared

## ADDED Requirements

### Requirement: Unpublish episode from target endpoint
The system SHALL provide a `DELETE /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/publications/{target}` endpoint. The endpoint SHALL verify the episode has a PUBLISHED publication for the given target, delegate removal to the publisher, update the publication status to `UNPUBLISHED`, and return the updated publication record.

#### Scenario: Successful unpublish from SoundCloud
- **WHEN** a `DELETE .../publications/soundcloud` request is received for an episode with a PUBLISHED SoundCloud publication
- **THEN** the system deletes the track from SoundCloud, rebuilds the playlist, updates the publication status to `UNPUBLISHED`, clears `external_id`, publishes an `episode.unpublished` SSE event, and returns HTTP 200 with the updated publication record

#### Scenario: Successful unpublish from FTP
- **WHEN** a `DELETE .../publications/ftp` request is received for an episode with a PUBLISHED FTP publication
- **THEN** the system deletes the MP3 file from the FTP server, regenerates and uploads feed.xml, updates the publication status to `UNPUBLISHED`, clears `external_id`, publishes an `episode.unpublished` SSE event, and returns HTTP 200

#### Scenario: Episode not published to target
- **WHEN** a `DELETE .../publications/soundcloud` request is received for an episode that is not PUBLISHED to SoundCloud
- **THEN** the system returns HTTP 404

#### Scenario: Unknown target
- **WHEN** a `DELETE .../publications/youtube` request is received for an unsupported target
- **THEN** the system returns HTTP 400

#### Scenario: Episode not found
- **WHEN** a `DELETE .../publications/soundcloud` request is received for an episode that does not exist
- **THEN** the system returns HTTP 404

### Requirement: FTP unpublish implementation
The `FtpPublisher` SHALL implement the `unpublish()` method. It SHALL connect to the FTP server, delete the episode's MP3 file from the remote episodes directory, and regenerate the feed.xml to exclude the unpublished episode.

#### Scenario: FTP file deletion
- **WHEN** `FtpPublisher.unpublish()` is called with a valid externalId
- **THEN** the system connects to the FTP server, deletes the MP3 file at the episode's remote path, and regenerates feed.xml

#### Scenario: FTP file already deleted
- **WHEN** the MP3 file does not exist on the FTP server
- **THEN** the system logs a warning and continues without error

### Requirement: SoundCloud playlist rebuild on unpublish
After unpublishing a SoundCloud track, the system SHALL rebuild the SoundCloud playlist to exclude the deleted track.

#### Scenario: Playlist rebuilt after unpublish
- **WHEN** a SoundCloud track is unpublished
- **THEN** the system rebuilds the playlist with all remaining published tracks

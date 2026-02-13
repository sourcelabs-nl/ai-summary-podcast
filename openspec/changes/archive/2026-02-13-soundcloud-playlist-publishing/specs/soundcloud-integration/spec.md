## ADDED Requirements

### Requirement: SoundCloud playlist creation
The `SoundCloudClient` SHALL provide a `createPlaylist` method that creates a new public playlist on SoundCloud via `POST https://api.soundcloud.com/playlists` with the user's access token. The request SHALL include a JSON body with the playlist `title` and an initial list of `track` IDs. The method SHALL return the created playlist's ID.

#### Scenario: Create playlist with initial track
- **WHEN** `createPlaylist` is called with title "The Daily AI Podcast" and track ID 2266108838
- **THEN** the SoundCloud API creates a public playlist containing the track and returns the playlist ID

#### Scenario: Create playlist fails
- **WHEN** `createPlaylist` is called and the SoundCloud API returns a non-2xx response
- **THEN** the method throws an exception with the SoundCloud error details

### Requirement: SoundCloud add track to playlist
The `SoundCloudClient` SHALL provide an `addTrackToPlaylist` method that adds a track to an existing SoundCloud playlist via `PUT https://api.soundcloud.com/playlists/{playlistId}` with the user's access token. The request SHALL include the updated list of track IDs. The method SHALL return the updated playlist's track list.

#### Scenario: Add track to existing playlist
- **WHEN** `addTrackToPlaylist` is called with playlist ID 12345 and track ID 6789
- **THEN** the SoundCloud API adds the track to the playlist

#### Scenario: Playlist not found (deleted on SoundCloud)
- **WHEN** `addTrackToPlaylist` is called with a playlist ID that no longer exists on SoundCloud
- **THEN** the method throws an exception indicating the playlist was not found (HTTP 404)

### Requirement: Automatic playlist management during publish
The `SoundCloudPublisher` SHALL manage playlists automatically during the publish flow. After uploading a track, the publisher SHALL check the podcast's `soundcloudPlaylistId`. If no playlist ID exists, the publisher SHALL create a new playlist containing the uploaded track and store the playlist ID on the podcast. If a playlist ID exists, the publisher SHALL add the uploaded track to the existing playlist. If adding to an existing playlist fails with a 404 (playlist deleted), the publisher SHALL create a new playlist and update the stored ID.

#### Scenario: First publish for podcast creates playlist
- **WHEN** an episode is published to SoundCloud for a podcast with no `soundcloudPlaylistId`
- **THEN** the publisher uploads the track, creates a new SoundCloud playlist containing the track, stores the playlist ID on the podcast, and returns the publish result

#### Scenario: Subsequent publish adds to existing playlist
- **WHEN** an episode is published to SoundCloud for a podcast with an existing `soundcloudPlaylistId`
- **THEN** the publisher uploads the track and adds it to the existing playlist

#### Scenario: Stale playlist ID triggers recreation
- **WHEN** an episode is published and adding to the existing playlist fails with HTTP 404
- **THEN** the publisher creates a new playlist containing the track, updates the podcast's `soundcloudPlaylistId`, and returns the publish result successfully

## MODIFIED Requirements

### Requirement: SoundCloud track upload
The `SoundCloudPublisher` SHALL implement the `EpisodePublisher` interface. It SHALL upload the episode's MP3 file to SoundCloud via `POST https://api.soundcloud.com/tracks` with `multipart/form-data` containing: `track[title]` (podcast name + episode date), `track[description]` (first 500 characters of script text), `track[tag_list]` (derived from podcast topic, space-separated, multi-word tags quoted), `track[sharing]` set to `"public"`, and `track[asset_data]` (the MP3 file). The upload SHALL use the user's decrypted OAuth access token as a Bearer token. After a successful upload, the publisher SHALL add the track to the podcast's SoundCloud playlist (creating one if it does not yet exist).

#### Scenario: Successful upload
- **WHEN** the publisher uploads an episode for a podcast named "Tech News" generated on 2026-02-13
- **THEN** the SoundCloud track is created with title "Tech News - 2026-02-13", description from the script, tags from the topic, the track is added to the podcast's playlist, and the system returns a `PublishResult` with the SoundCloud track ID and permalink URL

#### Scenario: Upload fails due to SoundCloud API error
- **WHEN** the upload request returns a non-2xx response from SoundCloud
- **THEN** the publisher throws an exception with the SoundCloud error message

#### Scenario: No OAuth connection for user
- **WHEN** the publisher is called for a user with no SoundCloud `oauth_connections` record
- **THEN** the publisher throws an exception indicating the user must connect their SoundCloud account first

## ADDED Requirements

### Requirement: SoundCloud get playlist
The `SoundCloudClient` SHALL provide a `getPlaylist` method that fetches an existing playlist's details via `GET https://api.soundcloud.com/playlists/{playlistId}` with the user's access token as a Bearer token. The response SHALL include the playlist's track list. The method SHALL return the playlist ID and the list of track IDs currently in the playlist.

#### Scenario: Fetch existing playlist with tracks
- **WHEN** `getPlaylist` is called with a valid playlist ID that contains tracks
- **THEN** the method returns the playlist ID and all current track IDs

#### Scenario: Fetch empty playlist
- **WHEN** `getPlaylist` is called with a valid playlist ID that contains no tracks
- **THEN** the method returns the playlist ID and an empty track list

#### Scenario: Playlist not found
- **WHEN** `getPlaylist` is called with a playlist ID that does not exist on SoundCloud
- **THEN** the method throws an `HttpClientErrorException.NotFound`

### Requirement: Playlist rebuild admin endpoint
The system SHALL provide a `POST /users/{userId}/podcasts/{podcastId}/playlist/rebuild` endpoint that rebuilds the podcast's SoundCloud playlist from all published episode publications. The endpoint SHALL query all `episode_publications` with `status = 'PUBLISHED'` and `target = 'soundcloud'` for episodes belonging to the podcast, collect their `externalId` values as SoundCloud track IDs, and call `SoundCloudPublisher.rebuildPlaylist()` to replace the playlist contents with the full list of track IDs. If no published SoundCloud tracks exist, the endpoint SHALL return HTTP 400. On success, the endpoint SHALL return the list of track IDs that were set on the playlist.

#### Scenario: Rebuild playlist with published tracks
- **WHEN** `POST /users/{userId}/podcasts/{podcastId}/playlist/rebuild` is called and the podcast has 3 published SoundCloud episodes
- **THEN** the endpoint queries all published SoundCloud publications, rebuilds the playlist with all 3 track IDs, and returns HTTP 200 with `{ "trackIds": [id1, id2, id3] }`

#### Scenario: No published tracks found
- **WHEN** `POST /users/{userId}/podcasts/{podcastId}/playlist/rebuild` is called and the podcast has no published SoundCloud episodes
- **THEN** the endpoint returns HTTP 400 with an error message

#### Scenario: Playlist does not exist on SoundCloud
- **WHEN** the rebuild is triggered and the stored playlist ID no longer exists on SoundCloud (404)
- **THEN** a new playlist is created with all track IDs and the podcast's `soundcloudPlaylistId` is updated

### Requirement: SoundCloud playlist rebuild
The `SoundCloudPublisher` SHALL provide a `rebuildPlaylist(podcast, userId, trackIds)` method that replaces the SoundCloud playlist contents with the given list of track IDs. If the podcast has an existing `soundcloudPlaylistId`, the method SHALL call `addTrackToPlaylist` with the full list of track IDs. If no playlist ID exists, the method SHALL create a new playlist with all track IDs and store the playlist ID on the podcast. If the existing playlist returns 404, the method SHALL create a new playlist and update the stored ID.

#### Scenario: Rebuild existing playlist
- **WHEN** `rebuildPlaylist` is called for a podcast with an existing playlist containing tracks [100, 200] and trackIds is [100, 200, 300]
- **THEN** `addTrackToPlaylist` is called with [100, 200, 300] and no new playlist is created

#### Scenario: Rebuild creates new playlist when none exists
- **WHEN** `rebuildPlaylist` is called for a podcast with no `soundcloudPlaylistId`
- **THEN** a new playlist is created with all track IDs and the playlist ID is stored on the podcast

#### Scenario: Rebuild creates new playlist when existing is deleted
- **WHEN** `rebuildPlaylist` is called and `addTrackToPlaylist` returns 404
- **THEN** a new playlist is created with all track IDs and the podcast's `soundcloudPlaylistId` is updated

### Requirement: Query published publications by podcast and target
The `EpisodePublicationRepository` SHALL provide a `findPublishedByPodcastIdAndTarget(podcastId, target)` method that returns all `EpisodePublication` records with `status = 'PUBLISHED'` and the given `target` for episodes belonging to the given podcast. The query SHALL join `episode_publications` with `episodes` on `episode_id` where `episodes.podcast_id = :podcastId`.

#### Scenario: Publications exist
- **WHEN** `findPublishedByPodcastIdAndTarget` is called with a podcast that has 2 published SoundCloud episodes
- **THEN** 2 `EpisodePublication` records are returned

#### Scenario: No publications
- **WHEN** `findPublishedByPodcastIdAndTarget` is called for a podcast with no published SoundCloud episodes
- **THEN** an empty list is returned

## MODIFIED Requirements

### Requirement: Automatic playlist management during publish
The `SoundCloudPublisher` SHALL manage playlists automatically during the publish flow. After uploading a track, the publisher SHALL check the podcast's `soundcloudPlaylistId`. If no playlist ID exists, the publisher SHALL create a new playlist containing the uploaded track and store the playlist ID on the podcast. If a playlist ID exists, the publisher SHALL fetch the existing playlist's current track IDs via `getPlaylist`, append the new track ID to the list, and update the playlist via `addTrackToPlaylist` with the complete track list (existing + new). If fetching or updating the existing playlist fails with a 404 (playlist deleted), the publisher SHALL create a new playlist and update the stored ID.

#### Scenario: First publish for podcast creates playlist
- **WHEN** an episode is published to SoundCloud for a podcast with no `soundcloudPlaylistId`
- **THEN** the publisher uploads the track, creates a new SoundCloud playlist containing the track, stores the playlist ID on the podcast, and returns the publish result

#### Scenario: Subsequent publish appends to existing playlist
- **WHEN** an episode is published to SoundCloud for a podcast with an existing `soundcloudPlaylistId` that contains tracks [100, 200]
- **THEN** the publisher fetches the current playlist tracks, calls `addTrackToPlaylist` with the full list [100, 200, newTrackId], and the existing tracks are preserved

#### Scenario: Stale playlist ID triggers recreation
- **WHEN** an episode is published and fetching the existing playlist fails with HTTP 404
- **THEN** the publisher creates a new playlist containing the track, updates the podcast's `soundcloudPlaylistId`, and returns the publish result successfully

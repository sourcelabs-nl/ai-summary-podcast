## MODIFIED Requirements

### Requirement: SoundCloud application configuration
The system SHALL resolve SoundCloud OAuth application credentials (`clientId`, `clientSecret`, `callbackUri`) by first checking `user_provider_configs` (category `PUBLISHING`, provider `soundcloud`) for the current user. If found, the encrypted JSON SHALL be decrypted and parsed. If not found, the system SHALL fall back to `app.soundcloud.client-id` and `app.soundcloud.client-secret` application properties (environment variables: `APP_SOUNDCLOUD_CLIENT_ID`, `APP_SOUNDCLOUD_CLIENT_SECRET`), with the callback URI derived from `app.feed.base-url` + `/oauth/soundcloud/callback`. If neither source provides credentials, the endpoints SHALL return HTTP 400 indicating SoundCloud credentials must be configured.

#### Scenario: SoundCloud credentials from user_provider_configs
- **WHEN** the SoundCloud OAuth flow is initiated and the user has a `PUBLISHING/soundcloud` config with `{"clientId":"abc","clientSecret":"xyz","callbackUri":"https://example.com/callback"}`
- **THEN** the system uses `clientId=abc`, `clientSecret=xyz`, and `callbackUri=https://example.com/callback`

#### Scenario: SoundCloud credentials from env var fallback
- **WHEN** the SoundCloud OAuth flow is initiated, the user has no DB config, but `APP_SOUNDCLOUD_CLIENT_ID` and `APP_SOUNDCLOUD_CLIENT_SECRET` are set
- **THEN** the system uses the env var values and derives the callback URI from `app.feed.base-url`

#### Scenario: SoundCloud credentials not configured anywhere
- **WHEN** the SoundCloud OAuth flow is initiated and neither DB config nor env vars are available
- **THEN** the system returns HTTP 400 indicating SoundCloud integration is not configured

### Requirement: Automatic playlist management during publish
The `SoundCloudPublisher` SHALL manage playlists automatically during the publish flow. After uploading a track, the publisher SHALL read the podcast's SoundCloud playlist ID from the `podcast_publication_targets` table (target `"soundcloud"`, config field `playlistId`). If no playlist ID exists, the publisher SHALL create a new playlist containing the uploaded track and store the playlist ID back to the target's config. If a playlist ID exists, the publisher SHALL fetch the existing playlist's current track IDs via `getPlaylist`, append the new track ID to the list, and update the playlist via `addTrackToPlaylist` with the complete track list (existing + new). If fetching or updating the existing playlist fails with a 404 (playlist deleted), the publisher SHALL create a new playlist and update the stored config.

#### Scenario: First publish for podcast creates playlist
- **WHEN** an episode is published to SoundCloud for a podcast whose `podcast_publication_targets` SoundCloud config has no `playlistId`
- **THEN** the publisher uploads the track, creates a new SoundCloud playlist containing the track, stores the playlist ID in the target's config, and returns the publish result

#### Scenario: Subsequent publish appends to existing playlist
- **WHEN** an episode is published to SoundCloud for a podcast whose SoundCloud target config has `playlistId` pointing to a playlist with tracks [100, 200]
- **THEN** the publisher fetches the current playlist tracks, calls `addTrackToPlaylist` with the full list [100, 200, newTrackId], and the existing tracks are preserved

#### Scenario: Stale playlist ID triggers recreation
- **WHEN** an episode is published and fetching the existing playlist fails with HTTP 404
- **THEN** the publisher creates a new playlist containing the track, updates the target config's `playlistId`, and returns the publish result successfully

### Requirement: SoundCloud playlist rebuild
The `SoundCloudPublisher` SHALL provide a `rebuildPlaylist(podcast, userId, trackIds)` method that replaces the SoundCloud playlist contents with the given list of track IDs. The playlist ID SHALL be read from and written to the `podcast_publication_targets` table (target `"soundcloud"`, config field `playlistId`). If the podcast has an existing playlist ID in the target config, the method SHALL call `addTrackToPlaylist` with the full list of track IDs. If no playlist ID exists, the method SHALL create a new playlist with all track IDs and store the playlist ID in the target config. If the existing playlist returns 404, the method SHALL create a new playlist and update the target config.

#### Scenario: Rebuild existing playlist
- **WHEN** `rebuildPlaylist` is called for a podcast whose SoundCloud target config has `playlistId` and trackIds is [100, 200, 300]
- **THEN** `addTrackToPlaylist` is called with [100, 200, 300] and no new playlist is created

#### Scenario: Rebuild creates new playlist when none exists
- **WHEN** `rebuildPlaylist` is called for a podcast whose SoundCloud target config has no `playlistId`
- **THEN** a new playlist is created with all track IDs and the playlist ID is stored in the target config

#### Scenario: Rebuild creates new playlist when existing is deleted
- **WHEN** `rebuildPlaylist` is called and `addTrackToPlaylist` returns 404
- **THEN** a new playlist is created with all track IDs and the target config's `playlistId` is updated

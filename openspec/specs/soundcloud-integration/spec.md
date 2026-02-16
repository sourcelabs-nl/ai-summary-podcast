# Capability: SoundCloud Integration

## Purpose

OAuth 2.1 PKCE-based SoundCloud integration enabling users to connect their SoundCloud account and publish podcast episodes as tracks.

## Requirements

### Requirement: SoundCloud OAuth authorization initiation
The system SHALL provide a `GET /users/{userId}/oauth/soundcloud/authorize` endpoint that generates a SoundCloud OAuth 2.1 authorization URL with PKCE. The endpoint SHALL generate a random code verifier, compute the code challenge (S256), generate a state parameter encoding the `userId` signed with HMAC-SHA256 (using the encryption master key), store the code verifier in a short-lived in-memory cache keyed by the state, and return a JSON response containing the `authorizationUrl`. The authorization URL SHALL point to `https://soundcloud.com/connect` with query parameters: `client_id`, `redirect_uri`, `response_type=code`, `code_challenge`, `code_challenge_method=S256`, `state`, and `scope=non-expiring`.

#### Scenario: Generate authorization URL
- **WHEN** a `GET /users/{userId}/oauth/soundcloud/authorize` request is received for an existing user
- **THEN** the system returns HTTP 200 with a JSON body containing an `authorizationUrl` pointing to SoundCloud's OAuth endpoint with all required parameters

#### Scenario: User not found
- **WHEN** a `GET /users/{userId}/oauth/soundcloud/authorize` request is received for a non-existing user
- **THEN** the system returns HTTP 404

#### Scenario: User already has a connected SoundCloud account
- **WHEN** a `GET /users/{userId}/oauth/soundcloud/authorize` request is received for a user who already has an active SoundCloud OAuth connection
- **THEN** the system returns HTTP 200 with a new authorization URL (allowing re-authorization to replace the existing connection)

### Requirement: SoundCloud OAuth callback
The system SHALL provide a `GET /oauth/soundcloud/callback` endpoint that handles the OAuth redirect from SoundCloud. The endpoint SHALL validate the `state` parameter's HMAC signature, extract the `userId`, retrieve the code verifier from the in-memory cache, exchange the authorization code for tokens via `POST https://api.soundcloud.com/oauth2/token`, encrypt the access and refresh tokens using `ApiKeyEncryptor`, and store them in the `oauth_connections` table. On success, the endpoint SHALL return an HTML page indicating the connection was successful.

#### Scenario: Successful callback
- **WHEN** SoundCloud redirects to `/oauth/soundcloud/callback?code=abc&state=xyz` with a valid state and code
- **THEN** the system exchanges the code for tokens, stores encrypted tokens in `oauth_connections`, and returns an HTML page saying "SoundCloud connected successfully! You can close this tab."

#### Scenario: Invalid state parameter
- **WHEN** the callback is received with a state parameter that has an invalid HMAC signature
- **THEN** the system returns HTTP 400 with an error page indicating the authorization request is invalid

#### Scenario: Expired state (code verifier not found)
- **WHEN** the callback is received with a valid state but the code verifier has expired from the cache
- **THEN** the system returns HTTP 400 with an error page indicating the authorization request has expired

#### Scenario: Token exchange fails
- **WHEN** the callback is received with a valid state and code but SoundCloud rejects the token exchange
- **THEN** the system returns an HTML error page indicating the connection failed

#### Scenario: Re-authorization replaces existing connection
- **WHEN** the callback completes for a user who already has an `oauth_connections` record for `"soundcloud"`
- **THEN** the existing record is updated with the new encrypted tokens and expiry

### Requirement: OAuth connection storage
The system SHALL store OAuth connections in an `oauth_connections` table with columns: `id` (INTEGER, auto-increment PK), `user_id` (TEXT, FK to users, NOT NULL), `provider` (TEXT, NOT NULL — e.g., `"soundcloud"`), `encrypted_access_token` (TEXT, NOT NULL), `encrypted_refresh_token` (TEXT, nullable), `expires_at` (TEXT, nullable — ISO-8601), `scopes` (TEXT, nullable), `created_at` (TEXT, NOT NULL — ISO-8601), `updated_at` (TEXT, NOT NULL — ISO-8601). A unique constraint SHALL exist on `(user_id, provider)`.

#### Scenario: Connection stored after successful OAuth
- **WHEN** a user completes the SoundCloud OAuth flow
- **THEN** a record is created in `oauth_connections` with encrypted tokens, expiry, and timestamps

#### Scenario: Tokens encrypted at rest
- **WHEN** tokens are stored in `oauth_connections`
- **THEN** both `encrypted_access_token` and `encrypted_refresh_token` are AES-256-GCM encrypted and Base64-encoded, not stored as plaintext

### Requirement: SoundCloud connection status endpoint
The system SHALL provide a `GET /users/{userId}/oauth/soundcloud/status` endpoint that returns whether the user has an active SoundCloud connection. The response SHALL include `connected` (boolean), and when connected: the `scopes` and `connectedAt` timestamp.

#### Scenario: User has active connection
- **WHEN** a `GET /users/{userId}/oauth/soundcloud/status` request is received for a user with a valid `oauth_connections` record for `"soundcloud"`
- **THEN** the system returns HTTP 200 with `{ "connected": true, "scopes": "non-expiring", "connectedAt": "..." }`

#### Scenario: User has no connection
- **WHEN** a `GET /users/{userId}/oauth/soundcloud/status` request is received for a user with no `oauth_connections` record for `"soundcloud"`
- **THEN** the system returns HTTP 200 with `{ "connected": false }`

#### Scenario: User not found
- **WHEN** a `GET /users/{userId}/oauth/soundcloud/status` request is received for a non-existing user
- **THEN** the system returns HTTP 404

### Requirement: Disconnect SoundCloud endpoint
The system SHALL provide a `DELETE /users/{userId}/oauth/soundcloud` endpoint that removes the user's SoundCloud OAuth connection by deleting the `oauth_connections` record.

#### Scenario: Disconnect existing connection
- **WHEN** a `DELETE /users/{userId}/oauth/soundcloud` request is received for a user with an active SoundCloud connection
- **THEN** the system deletes the `oauth_connections` record and returns HTTP 204

#### Scenario: No connection to disconnect
- **WHEN** a `DELETE /users/{userId}/oauth/soundcloud` request is received for a user with no SoundCloud connection
- **THEN** the system returns HTTP 404

### Requirement: Token refresh before API calls
The system SHALL check the access token's `expires_at` before each SoundCloud API call. If the token expires within 5 minutes, the system SHALL refresh it using the refresh token via `POST https://api.soundcloud.com/oauth2/token` (grant_type=refresh_token), encrypt and store the new tokens, and use the new access token for the API call. If the refresh fails, the system SHALL mark the connection status as expired and return a clear error to the caller.

#### Scenario: Token still valid
- **WHEN** a SoundCloud API call is made and the access token expires in more than 5 minutes
- **THEN** the system uses the existing access token without refreshing

#### Scenario: Token near expiry, refresh succeeds
- **WHEN** a SoundCloud API call is made and the access token expires in less than 5 minutes
- **THEN** the system refreshes the token, stores the new encrypted tokens, and uses the new access token

#### Scenario: Token expired and refresh fails
- **WHEN** a SoundCloud API call is made, the token is expired, and the refresh request fails (e.g., user revoked access)
- **THEN** the system returns an error indicating the SoundCloud connection needs to be re-authorized

### Requirement: SoundCloud track upload
The `SoundCloudPublisher` SHALL implement the `EpisodePublisher` interface. It SHALL upload the episode's MP3 file to SoundCloud via `POST https://api.soundcloud.com/tracks` with `multipart/form-data` containing: `track[title]` (podcast name + episode date), `track[description]` (first 500 characters of script text), `track[tag_list]` (derived from podcast topic, space-separated, multi-word tags quoted), `track[sharing]` set to `"public"`, `track[permalink]` (URL-safe slug derived from podcast name and episode date), and `track[asset_data]` (the MP3 file). The permalink slug SHALL be computed by concatenating the podcast name and date with a hyphen, converting to lowercase, replacing non-alphanumeric characters (except hyphens) with hyphens, collapsing consecutive hyphens, and trimming leading/trailing hyphens. The upload SHALL use the user's decrypted OAuth access token as a Bearer token. After a successful upload, the publisher SHALL add the track to the podcast's SoundCloud playlist (creating one if it does not yet exist).

#### Scenario: Successful upload with permalink
- **WHEN** the publisher uploads an episode for a podcast named "Tech News" generated on 2026-02-13
- **THEN** the SoundCloud track is created with title "Tech News - 2026-02-13", permalink `"tech-news-2026-02-13"`, description from the script, tags from the topic, the track is added to the podcast's playlist, and the system returns a `PublishResult` with the SoundCloud track ID and permalink URL

#### Scenario: Upload fails due to SoundCloud API error
- **WHEN** the upload request returns a non-2xx response from SoundCloud
- **THEN** the publisher throws an exception with the SoundCloud error message

#### Scenario: No OAuth connection for user
- **WHEN** the publisher is called for a user with no SoundCloud `oauth_connections` record
- **THEN** the publisher throws an exception indicating the user must connect their SoundCloud account first

### Requirement: SoundCloud playlist creation
The `SoundCloudClient` SHALL provide a `createPlaylist` method that creates a new public playlist on SoundCloud via `POST https://api.soundcloud.com/playlists` with the user's access token. The request SHALL include a JSON body with the playlist `title` and an initial list of track IDs serialized as strings. The method SHALL return the created playlist's ID.

#### Scenario: Create playlist with initial track
- **WHEN** `createPlaylist` is called with title "The Daily AI Podcast" and track ID 2266108838
- **THEN** the SoundCloud API creates a public playlist containing the track and returns the playlist ID

#### Scenario: Create playlist fails
- **WHEN** `createPlaylist` is called and the SoundCloud API returns a non-2xx response
- **THEN** the method throws an exception with the SoundCloud error details

### Requirement: SoundCloud add track to playlist
The `SoundCloudClient` SHALL provide an `addTrackToPlaylist` method that adds a track to an existing SoundCloud playlist via `PUT https://api.soundcloud.com/playlists/{playlistId}` with the user's access token. The request SHALL include the track IDs serialized as strings. The method SHALL return the updated playlist response.

#### Scenario: Add track to existing playlist
- **WHEN** `addTrackToPlaylist` is called with playlist ID 12345 and track ID 6789
- **THEN** the SoundCloud API adds the track to the playlist

#### Scenario: Playlist not found (deleted on SoundCloud)
- **WHEN** `addTrackToPlaylist` is called with a playlist ID that no longer exists on SoundCloud
- **THEN** the method throws an exception indicating the playlist was not found (HTTP 404)

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

### Requirement: SoundCloud update track metadata
The `SoundCloudClient` SHALL provide an `updateTrack` method that updates a track's permalink via `PUT https://api.soundcloud.com/tracks/{trackId}` with the user's access token as a Bearer token. The request SHALL include a JSON body with `track.permalink` set to the new permalink slug. The method SHALL return the updated track response.

#### Scenario: Update track permalink
- **WHEN** `updateTrack` is called with track ID 100 and permalink `"tech-news-2026-02-13"`
- **THEN** the SoundCloud API updates the track's permalink and returns the updated track

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

### Requirement: Update track permalinks during rebuild
The `SoundCloudPublisher` SHALL provide an `updateTrackPermalinks(podcast, userId, episodes, publications)` method that updates the permalink of each published SoundCloud track. For each publication, the method SHALL look up the corresponding episode, derive the episode date from `generatedAt`, compute the permalink slug using the same `buildPermalink` logic as the publish flow (podcast name + date, lowercased, non-alphanumeric replaced with hyphens), and call `SoundCloudClient.updateTrack` with the track ID and new permalink. The playlist rebuild endpoint SHALL call `updateTrackPermalinks` before rebuilding the playlist.

#### Scenario: Update permalinks for two published tracks
- **WHEN** `updateTrackPermalinks` is called with a podcast named "Tech News" and two episodes (2026-02-13, 2026-02-14) with SoundCloud track IDs 100 and 200
- **THEN** `updateTrack` is called with track 100 and permalink `"tech-news-2026-02-13"`, and track 200 and permalink `"tech-news-2026-02-14"`

#### Scenario: Publication with missing episode is skipped
- **WHEN** `updateTrackPermalinks` is called and a publication references an episode ID not in the provided episodes list
- **THEN** that publication is skipped without error

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

### Requirement: Query published publications by podcast and target
The `EpisodePublicationRepository` SHALL provide a `findPublishedByPodcastIdAndTarget(podcastId, target)` method that returns all `EpisodePublication` records with `status = 'PUBLISHED'` and the given `target` for episodes belonging to the given podcast. The query SHALL join `episode_publications` with `episodes` on `episode_id` where `episodes.podcast_id = :podcastId`.

#### Scenario: Publications exist
- **WHEN** `findPublishedByPodcastIdAndTarget` is called with a podcast that has 2 published SoundCloud episodes
- **THEN** 2 `EpisodePublication` records are returned

#### Scenario: No publications
- **WHEN** `findPublishedByPodcastIdAndTarget` is called for a podcast with no published SoundCloud episodes
- **THEN** an empty list is returned

### Requirement: SoundCloud application configuration
The system SHALL require SoundCloud OAuth application credentials configured via `app.soundcloud.client-id` and `app.soundcloud.client-secret` application properties (environment variables: `APP_SOUNDCLOUD_CLIENT_ID`, `APP_SOUNDCLOUD_CLIENT_SECRET`). The redirect URI SHALL be derived from `app.feed.base-url` + `/oauth/soundcloud/callback`.

#### Scenario: SoundCloud credentials configured
- **WHEN** the application starts with `APP_SOUNDCLOUD_CLIENT_ID` and `APP_SOUNDCLOUD_CLIENT_SECRET` set
- **THEN** the SoundCloud OAuth endpoints are available and functional

#### Scenario: SoundCloud credentials not configured
- **WHEN** the application starts without SoundCloud credentials
- **THEN** the SoundCloud OAuth and publishing endpoints SHALL return HTTP 503 indicating SoundCloud integration is not configured

### Requirement: SoundCloudClient uses Spring-managed RestTemplate
The `SoundCloudClient` SHALL use a `RestTemplate` built from Spring Boot's `RestTemplateBuilder` instead of a plain `RestTemplate()` constructor. This ensures proper auto-configured message converters (Jackson 3) are available.

### Requirement: Cascade delete OAuth connections with user
When a user is deleted, all of the user's OAuth connections SHALL be deleted as part of the cascade.

#### Scenario: Delete user removes OAuth connections
- **WHEN** a user with a SoundCloud OAuth connection is deleted
- **THEN** the `oauth_connections` record is removed

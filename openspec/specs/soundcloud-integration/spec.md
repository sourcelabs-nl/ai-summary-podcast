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

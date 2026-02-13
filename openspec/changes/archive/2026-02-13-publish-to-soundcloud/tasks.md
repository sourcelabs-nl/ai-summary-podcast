## 1. Database Migrations

- [x] 1.1 Create `V13__add_oauth_connections.sql` migration with `oauth_connections` table (id, user_id, provider, encrypted_access_token, encrypted_refresh_token, expires_at, scopes, created_at, updated_at) and unique constraint on (user_id, provider)
- [x] 1.2 Create `V14__add_episode_publications.sql` migration with `episode_publications` table (id, episode_id, target, status, external_id, external_url, error_message, published_at, created_at) and unique constraint on (episode_id, target)

## 2. Data Model & Repositories

- [x] 2.1 Create `OAuthConnection` entity data class and `OAuthConnectionRepository` with methods: findByUserIdAndProvider, deleteByUserIdAndProvider, deleteByUserId
- [x] 2.2 Create `EpisodePublication` entity data class and `EpisodePublicationRepository` with methods: findByEpisodeId, findByEpisodeIdAndTarget, deleteByEpisodeId

## 3. SoundCloud Configuration

- [x] 3.1 Add `app.soundcloud.client-id` and `app.soundcloud.client-secret` to `AppProperties` with corresponding properties class
- [x] 3.2 Add default (empty) SoundCloud config values to `application.yaml`

## 4. OAuth Connection Service

- [x] 4.1 Create `OAuthConnectionService` with methods: storeConnection (encrypts tokens via ApiKeyEncryptor), getConnection (decrypts tokens), deleteConnection, getStatus
- [x] 4.2 Add cascade delete of OAuth connections in user deletion flow

## 5. SoundCloud OAuth Flow

- [x] 5.1 Create `SoundCloudOAuthController` with `GET /users/{userId}/oauth/soundcloud/authorize` endpoint — generates PKCE code verifier/challenge, creates signed state parameter (HMAC-SHA256), stores verifier in in-memory cache, returns authorization URL
- [x] 5.2 Create `GET /oauth/soundcloud/callback` endpoint — validates state HMAC, retrieves code verifier from cache, exchanges code for tokens via SoundCloud API, stores encrypted tokens via OAuthConnectionService, returns success HTML page
- [x] 5.3 Create `GET /users/{userId}/oauth/soundcloud/status` endpoint — returns connection status (connected boolean, scopes, connectedAt)
- [x] 5.4 Create `DELETE /users/{userId}/oauth/soundcloud` endpoint — disconnects by deleting the OAuth connection
- [x] 5.5 Add guard that returns HTTP 503 when SoundCloud client-id/client-secret are not configured

## 6. SoundCloud API Client

- [x] 6.1 Create `SoundCloudClient` service that handles HTTP calls to SoundCloud API — token exchange (POST /oauth2/token), token refresh, and track upload (POST /tracks with multipart/form-data)
- [x] 6.2 Implement proactive token refresh logic — check expires_at before API calls, refresh if within 5 minutes of expiry, update stored tokens

## 7. Publisher Abstraction

- [x] 7.1 Create `EpisodePublisher` interface with `publish(episode, podcast, userId): PublishResult` and `targetName(): String` methods
- [x] 7.2 Create `PublisherRegistry` Spring component that collects all `EpisodePublisher` beans and provides `getPublisher(target)` lookup
- [x] 7.3 Create `PublishingService` that orchestrates: validates episode state, checks for duplicate publication, delegates to publisher, creates/updates `episode_publications` record

## 8. SoundCloud Publisher Implementation

- [x] 8.1 Create `SoundCloudPublisher` implementing `EpisodePublisher` — retrieves OAuth tokens, uploads MP3 via SoundCloudClient, maps metadata (title from podcast name + date, description from script, tags from topic)
- [x] 8.2 Add cascade delete of episode publications in episode cleanup flow

## 9. Publishing API Endpoints

- [x] 9.1 Create `PublishingController` with `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/publish/{target}` endpoint
- [x] 9.2 Create `GET /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/publications` endpoint

## 10. Tests

- [x] 10.1 Write unit tests for `OAuthConnectionService` (encrypt/decrypt tokens, store/retrieve/delete connections)
- [x] 10.2 Write unit tests for `SoundCloudTokenManager` (token exchange, token refresh, track upload) using MockK to mock HTTP calls
- [x] 10.3 Write unit tests for `PublisherRegistry` (register, lookup known target, lookup unknown target)
- [x] 10.4 Write unit tests for `PublishingService` (happy path, episode not generated, already published, unknown target)
- [x] 10.5 Write unit tests for `SoundCloudPublisher` (metadata mapping, successful publish, missing OAuth connection)
- [x] 10.6 Write integration tests for OAuth controller endpoints (authorize URL generation, callback flow, status, disconnect)
- [x] 10.7 Write integration tests for publishing controller endpoints (publish, list publications)

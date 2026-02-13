## Context

The podcast pipeline currently generates episodes (MP3 audio + script text) and serves them through a self-hosted RSS feed and static file server. There is no concept of publishing to external platforms. Each user has their own podcasts and can configure per-user provider credentials (LLM, TTS) encrypted with AES-256-GCM via `ApiKeyEncryptor`.

We're adding SoundCloud as the first external publishing target, behind a general `EpisodePublisher` interface that future targets can implement.

SoundCloud requires OAuth 2.1 with PKCE (Authorization Code flow) for track uploads. The API accepts `POST /tracks` with `multipart/form-data` (up to 500 MB), and supports metadata updates via `PUT /tracks/:id` but does **not** allow replacing audio on existing tracks.

## Goals / Non-Goals

**Goals:**
- Enable users to manually publish individual episodes to their own SoundCloud account
- Build a general publisher abstraction that SoundCloud implements as the first target
- Securely store per-user OAuth tokens (access + refresh) with the existing encryption infrastructure
- Track publication status per episode per target

**Non-Goals:**
- Automatic publishing (episodes are published only on explicit user action)
- Unpublishing / deleting tracks from SoundCloud (manage via SoundCloud directly)
- Publishing to other platforms (YouTube, Spotify, etc.) — future work using the same abstraction
- Artwork upload (metadata limited to title, description, tags for now)
- SoundCloud playlist management

## Decisions

### 1. Publisher Abstraction

**Decision:** Create an `EpisodePublisher` interface with a `publish(episode, podcast, user)` method. Each implementation is a Spring bean identified by a target name string (e.g., `"soundcloud"`). A `PublisherRegistry` collects all implementations and dispatches by target name.

**Why not a simpler approach (just a SoundCloud service)?** The user explicitly wants a general abstraction. A registry pattern keeps the controller/service layer target-agnostic — adding a new target means adding a new `EpisodePublisher` bean without touching existing code.

**Alternatives considered:**
- Strategy pattern with enum — too rigid, requires code change to add targets
- Event-based (publish event, listeners per target) — over-engineered for manual per-episode publishing

### 2. OAuth Token Storage

**Decision:** Store tokens in a new `oauth_connections` table with columns: `id`, `user_id`, `provider` (e.g., `"soundcloud"`), `encrypted_access_token`, `encrypted_refresh_token`, `expires_at`, `scopes`, `created_at`, `updated_at`. Tokens encrypted using the existing `ApiKeyEncryptor` (AES-256-GCM with `APP_ENCRYPTION_MASTER_KEY`).

**Why a separate table instead of extending `user_provider_configs`?** OAuth connections have different semantics — they have expiry, refresh tokens, scopes, and a lifecycle (connect/disconnect/refresh). Mixing them into the API key table would conflate two different credential models.

**Why not a generic "credentials" table?** That would require a flexible schema (JSON blobs, type discriminators) that's harder to query and validate. Separate tables are simpler and more explicit.

### 3. OAuth Flow

**Decision:** Redirect-based OAuth 2.1 with PKCE.

Flow:
```
GET /users/{userId}/oauth/soundcloud/authorize
  → Returns { authorizationUrl: "https://soundcloud.com/connect?..." }

User visits URL in browser → logs in → consents

SoundCloud redirects to:
GET /oauth/soundcloud/callback?code=xxx&state=yyy
  → Exchanges code for tokens
  → Stores encrypted tokens in oauth_connections
  → Returns HTML page saying "Connected! You can close this tab."
```

The `state` parameter encodes the `userId` (signed with HMAC to prevent tampering) so the callback can associate the authorization with the correct user.

**Why PKCE?** SoundCloud requires OAuth 2.1 which mandates PKCE. The code verifier is stored server-side in a short-lived cache (or the state parameter) during the flow.

### 4. Token Refresh

**Decision:** Refresh tokens proactively before API calls. Before each SoundCloud API call, check if the access token's `expires_at` is within 5 minutes. If so, refresh using the stored refresh token and update the database. If refresh fails (e.g., user revoked access), mark the connection as `EXPIRED` and return a clear error.

**Why not refresh on 401?** Proactive refresh avoids a failed API call + retry, which is especially wasteful for large file uploads.

### 5. Publication Tracking

**Decision:** New `episode_publications` table with columns: `id`, `episode_id`, `target` (e.g., `"soundcloud"`), `status` (PENDING, PUBLISHED, FAILED), `external_id` (SoundCloud track ID), `external_url`, `error_message`, `published_at`, `created_at`. Composite unique constraint on `(episode_id, target)` — an episode can be published to each target at most once.

**Why a separate table?** Supports multiple targets per episode without modifying the episode entity. The general publisher abstraction needs target-agnostic tracking.

### 6. SoundCloud Track Metadata

**Decision:** Map episode/podcast data to SoundCloud track fields:
- `track[title]` → `"{podcast.name} - {episode.generatedAt as LocalDate}"`  (matches RSS feed title format)
- `track[description]` → First 500 characters of `episode.scriptText`
- `track[tag_list]` → Derived from `podcast.topic` (space-separated, quoted multi-word tags)
- `track[sharing]` → `"public"`

### 7. API Endpoints

New endpoints:
- `GET /users/{userId}/oauth/soundcloud/authorize` — Initiate OAuth flow
- `GET /oauth/soundcloud/callback` — OAuth callback (no userId in path — it's in the state param)
- `DELETE /users/{userId}/oauth/soundcloud` — Disconnect SoundCloud
- `GET /users/{userId}/oauth/soundcloud/status` — Check connection status
- `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/publish/{target}` — Publish episode to target
- `GET /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/publications` — List publication statuses

## Risks / Trade-offs

- **SoundCloud API rate limits** (50 tokens/12h per app, 30 tokens/1h per IP) → Mitigation: Publishing is manual and per-episode, so volume is naturally low. Add clear error messaging if rate-limited.
- **OAuth token expiry during long operations** → Mitigation: Proactive refresh before upload. Track uploads for typical episode sizes (5-30 MB) should complete well within token lifetime.
- **SoundCloud API changes or deprecation** → Mitigation: Isolate all SoundCloud-specific code in `SoundCloudPublisher` and `SoundCloudClient`. Changes only affect these classes.
- **500 MB upload limit** → Not a practical risk: podcast episodes are typically 5-30 MB MP3 files.
- **Cannot replace audio on existing SoundCloud tracks** → Mitigation: Episode audio is immutable in our system too, so this aligns. If re-publishing is needed, the user would need to delete the track on SoundCloud and re-publish.
- **PKCE code verifier storage** → Store in a short-lived in-memory cache (ConcurrentHashMap with TTL) keyed by the state parameter. Acceptable for a self-hosted single-instance app.

## Open Questions

- Should the SoundCloud client ID and secret be configured globally (in `application.yaml`) or per-user? → **Recommendation:** Globally, since registering a SoundCloud app requires developer account setup. All users share the same app registration.
## Why

The podcast pipeline currently only serves episodes through a self-hosted RSS feed and static file server. Users who want broader distribution — such as publishing to SoundCloud — must manually download and re-upload episodes. Adding SoundCloud as a publishing target lets users reach their SoundCloud audience directly from the app, with a general publisher abstraction that makes future targets (YouTube, Spotify hosting, etc.) easy to add.

## What Changes

- Introduce a general **episode publishing** abstraction (`EpisodePublisher` interface) that any distribution target can implement.
- Add **SoundCloud as the first publisher implementation**, uploading MP3 episodes via the SoundCloud API (`POST /tracks` with multipart/form-data).
- Add **per-user OAuth 2.1 (PKCE) integration with SoundCloud**, using a redirect-based flow (`/oauth/soundcloud/callback`) so each user can connect their own SoundCloud account.
- Store **OAuth tokens (access + refresh) encrypted in the database**, using the existing AES-256-GCM encryption with `APP_ENCRYPTION_MASTER_KEY`.
- Track **episode publication status per target** in a new `episode_publications` table (episode_id, target, status, external_id, published_at).
- Publishing is **manual per-episode** — users explicitly trigger "Publish to SoundCloud" for individual episodes.
- Upload **title, description (from script excerpt), and tags** (derived from podcast topic/sources) as SoundCloud track metadata.

## Capabilities

### New Capabilities

- `episode-publishing`: General publisher abstraction — interface for publishing episodes to external targets, publication status tracking via `episode_publications` table, and API endpoints for triggering and querying publication status.
- `soundcloud-integration`: SoundCloud-specific implementation — OAuth 2.1 PKCE flow for connecting user accounts, token storage and refresh, track upload via SoundCloud API, and metadata mapping (title, description, tags).

### Modified Capabilities

- `database-migrations`: Add migrations for `oauth_connections` table (per-user OAuth tokens) and `episode_publications` table (publication tracking).
- `user-api-keys`: Extend the concept of per-user external service credentials to include OAuth-based connections (not just API keys).

## Impact

- **Database**: Two new tables (`oauth_connections`, `episode_publications`) requiring Flyway migrations.
- **New API endpoints**: OAuth flow initiation + callback, publish episode, get publication status.
- **New dependency**: HTTP client calls to SoundCloud API (track upload, token exchange/refresh).
- **Configuration**: New `app.soundcloud.client-id` and `app.soundcloud.client-secret` properties.
- **No breaking changes**: Existing episode lifecycle, feed generation, and all current APIs remain unchanged.
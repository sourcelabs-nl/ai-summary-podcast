## Context

Publishing is currently SoundCloud-only. Credentials live in `AppProperties` (`app.soundcloud.client-id/client-secret`), OAuth tokens in `oauth_connections`, and the playlist ID is a dedicated column on the `podcasts` table. The `EpisodePublisher` interface and `PublisherRegistry` already support multiple publishers, but there's no way to configure which targets a podcast uses or store credentials for new targets.

The `user_provider_configs` table stores encrypted per-user credentials with a `(user_id, category, provider)` composite key — currently for `LLM` and `TTS` categories.

## Goals / Non-Goals

**Goals:**
- Generic per-podcast publication target configuration (enable/disable targets, store target-specific config)
- User-level publication credential storage for FTP and SoundCloud, reusing `user_provider_configs`
- FTP(S) publisher that uploads MP3, sources.md, feed.xml, and podcast image
- Podcast image upload with validation (image type, max 1MB)
- Per-episode `sources.md` generation (standalone markdown with podcast reference, summary, and sources)
- Feed XML improvements: simplified title, `<image>` tag, recap-only descriptions with sources.md link
- Test connection endpoint (validate credentials before saving)
- Frontend for both credential management and per-podcast target assignment
- Migrate `soundcloudPlaylistId` off the `Podcast` entity into the generic model
- Regenerate existing episode show notes to the new format (recap only, no inline sources)

**Non-Goals:**
- Auto-publishing (episodes still published manually via the publish wizard)
- Other publication targets beyond FTP and SoundCloud
- Per-podcast credential overrides (credentials are user-level only)

## Decisions

### 1. Reuse `user_provider_configs` for publication credentials

**Decision:** Add `PUBLISHING` to the `ApiKeyCategory` enum. Store FTP and SoundCloud credentials as JSON-encoded strings in the existing `encrypted_api_key` column.

| Provider | Stored JSON |
|----------|-------------|
| `ftp` | `{"host":"...","port":21,"username":"...","password":"...","useTls":true}` |
| `soundcloud` | `{"clientId":"...","clientSecret":"...","callbackUri":"..."}` |

**Rationale:** The table already handles encryption, CRUD, and cascade deletes. Adding a category is a one-line enum change. The `base_url` column is not meaningful for publishing providers — it stays null.

**Alternative considered:** New `publication_credentials` table. Rejected because it would duplicate the encryption and CRUD patterns already in `user_provider_configs`.

### 2. New `podcast_publication_targets` table for per-podcast config

**Decision:** Create a new table:

```sql
CREATE TABLE podcast_publication_targets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    podcast_id TEXT NOT NULL REFERENCES podcasts(id) ON DELETE CASCADE,
    target TEXT NOT NULL,
    config TEXT NOT NULL DEFAULT '{}',
    enabled INTEGER NOT NULL DEFAULT 0,
    UNIQUE(podcast_id, target)
);
```

The `config` column stores a JSON object with target-specific settings:
- SoundCloud: `{"playlistId": "..."}`
- FTP: `{"remotePath": "/podcasts/my-show/", "publicUrl": "https://podcast.example.com/podcasts/my-show/"}`

**Rationale:** Avoids polluting the `Podcast` entity with per-target columns. The unique constraint on `(podcast_id, target)` prevents duplicate targets. `ON DELETE CASCADE` from `podcasts` handles cleanup.

### 3. SoundCloud credential resolution with env var fallback

**Decision:** The SoundCloud OAuth flow and publisher resolve `clientId`/`clientSecret` by:
1. Looking up `user_provider_configs` where `category=PUBLISHING, provider=soundcloud`
2. Falling back to `AppProperties` (`app.soundcloud.client-id/client-secret`) if no DB config exists

**Rationale:** Preserves backward compatibility for existing deployments using env vars. Users who configure credentials via the UI get DB-stored values.

### 4. Test connection endpoint accepts unsaved credentials

**Decision:** `POST /users/{userId}/publishing/test/{target}` accepts the credential payload in the request body and tests connectivity without persisting. For SoundCloud, the endpoint tests the existing OAuth connection and returns quota info (no request body needed).

**Rationale:** Users should verify credentials before saving. Sending unsaved values avoids a save-test-delete cycle on failure.

### 5. FTP publisher implementation

**Decision:** Use Apache Commons Net `FTPSClient`/`FTPClient` for FTP(S) support. The publisher:
1. Resolves FTP credentials from `user_provider_configs`
2. Reads `remotePath` and `publicUrl` from `podcast_publication_targets` config
3. Connects, authenticates
4. Generates and uploads `sources.md` for the episode
5. Uploads the MP3 file
6. Regenerates and uploads `feed.xml` (using `publicUrl` as base URL for enclosure URLs)
7. Uploads podcast image (if exists)
8. Disconnects

The `publicUrl` is the public-facing URL where uploaded files are accessible (e.g., `https://podcast.example.com/shows/tech/`). It is used for enclosure URLs in `feed.xml` and for the sources.md link in episode descriptions.

**Rationale:** Apache Commons Net is the standard Java FTP library, well-maintained, supports both FTP and FTPS (explicit TLS).

### 5a. Episode sources.md generation

**Decision:** Generate a standalone markdown file per episode stored at `data/episodes/{podcastId}/{slug}-sources.md`. The file contains:
- Podcast name as heading
- Episode date
- Recap summary
- Formatted source list with titles and URLs

The file is generated during the episode creation pipeline (alongside show notes) and uploaded by the FTP publisher.

**Rationale:** Keeps sources accessible without bloating the RSS description. Spotify and other podcast apps that don't handle long descriptions gracefully can just show the recap + a link.

### 5b. Podcast image upload and validation

**Decision:** New endpoint `POST /users/{userId}/podcasts/{podcastId}/image` accepts multipart file upload. Validation:
- Must be an image (JPEG, PNG, WebP — verify content type and magic bytes)
- Max 1MB file size

Stored at `data/episodes/{podcastId}/podcast-image.{ext}`. The FTP publisher uploads the image alongside other files. The feed generator includes it in the `<image>` tag using the `publicUrl`.

**Rationale:** Podcast directories (Apple, Spotify) require artwork. Validation prevents abuse and accidental non-image uploads.

### 5c. Feed XML changes

**Decision:** Three changes to the feed generator:
1. **Title**: Simplified from `"{app.feed.title} - {userName} - {podcastName}"` to just `"{podcastName}"`
2. **Image**: Add `<image><url>{publicUrl}/podcast-image.jpg</url><title>{podcastName}</title></image>` when a podcast image exists
3. **Episode descriptions**: Change from recap + inline sources to recap only + link to sources.md: `"{recap}\n\nSources: {publicUrl}/{slug}-sources.md"`

When no FTP target is configured (no `publicUrl`), the feed falls back to the existing `app.feed.base-url` or `app.feed.static-base-url`. Episode descriptions without a `publicUrl` still use recap-only format (no sources link).

**Rationale:** Inline source lists break Spotify feed parsing. A linked sources.md keeps the information accessible without cluttering the description.

### 5d. Existing episode show notes regeneration

**Decision:** A one-time Flyway migration or startup task regenerates `showNotes` for all existing episodes. The new format is recap-only (no inline sources). Episodes that have a `recap` field get their show notes updated. Episodes without a recap keep their existing show notes unchanged.

**Rationale:** Consistency — all episodes should have the same description format. Old episodes with inline sources would still break Spotify.

### 6. Publishing service checks target enablement

**Decision:** Before publishing, `PublishingService` verifies that the podcast has an enabled `podcast_publication_targets` entry for the requested target. If not configured or disabled, return HTTP 400.

**Rationale:** Prevents accidental publishing to unconfigured targets.

### 7. Frontend: two separate UIs

**Decision:**
- **User Settings > Publication Credentials** — configure/test FTP and SoundCloud credentials. SoundCloud also shows OAuth connection status.
- **Podcast Settings > Publication Targets** — enable/disable targets per podcast, configure target-specific settings. Targets without user credentials are greyed out with a hint.

**Rationale:** Credentials are user-level (shared across podcasts), targets are podcast-level. Separating them matches the data model.

## Risks / Trade-offs

**[JSON in `encrypted_api_key` column]** → The column was designed for a single API key string, not JSON. Mitigation: the column is opaque encrypted text — the shape of the plaintext doesn't matter to the storage layer. Parsing happens in the service layer.

**[SoundCloud credential migration]** → Existing deployments use env vars. Mitigation: env var fallback ensures no breakage. Users can optionally migrate to DB-stored credentials via the new UI.

**[`soundcloudPlaylistId` migration]** → Must move existing data. Mitigation: Flyway migration copies non-null values into `podcast_publication_targets` before dropping the column. Rollback: the migration is additive (new table + data copy), column drop is the final step.

## Migration Plan

1. Add `PUBLISHING` to `ApiKeyCategory` enum
2. Create `podcast_publication_targets` table (Flyway migration)
3. Migrate `soundcloudPlaylistId` data into `podcast_publication_targets` (same migration)
4. Drop `soundcloudPlaylistId` column from `podcasts` (same migration — SQLite requires table recreation)
5. Update `SoundCloudPublisher` to read playlist ID from `podcast_publication_targets` instead of `Podcast.soundcloudPlaylistId`
6. Update `SoundCloudClient`/`SoundCloudOAuthController` to resolve credentials from `user_provider_configs` with env var fallback
7. Add FTP publisher, test endpoint, and CRUD endpoints for publication targets
8. Frontend changes

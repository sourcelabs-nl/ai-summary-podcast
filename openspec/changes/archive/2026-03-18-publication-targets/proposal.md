## Why

Publishing is currently hardcoded to SoundCloud. Adding FTP(S) as a second publication target requires a generic configuration model — both for user-level credentials and per-podcast target assignment. Without this, every new target would need bespoke columns, env vars, and UI pages.

## What Changes

- **New `podcast_publication_targets` table** — per-podcast publication target configuration (target name, JSON config, enabled flag), replacing the hardcoded `soundcloudPlaylistId` column on `podcasts`.
- **Publication credentials in `user_provider_configs`** — store SoundCloud (`clientId`, `clientSecret`, `callbackUri`) and FTP (`host`, `port`, `username`, `password`, `useTls`) credentials as encrypted JSON under category `PUBLISHING`. SoundCloud env vars remain as fallback.
- **FTP(S) publisher** — new `EpisodePublisher` implementation that uploads MP3, `sources.md`, `feed.xml`, and podcast image to a configurable remote path via FTP or FTPS. FTP target config includes `remotePath` and `publicUrl` (the public-facing URL where uploaded files are accessible).
- **Feed XML changes** — simplified title to `{podcastName}` only, `<image>` tag for podcast artwork, episode description uses recap only + link to `sources.md` instead of inline sources list.
- **Podcast image upload** — `POST /users/{userId}/podcasts/{podcastId}/image` endpoint to upload podcast artwork (validated: must be image, max 1MB). Stored in podcast data folder. Used in feed `<image>` tag and uploaded to FTP.
- **Episode `sources.md` generation** — standalone markdown file per episode with podcast reference, episode summary, and formatted source list. Uploaded alongside MP3 via FTP, linked in episode description.
- **Show notes format change** — episode `showNotes` no longer contains inline sources. Updated to recap + link to `sources.md`. Existing episodes regenerated to the new format.
- **Test connection endpoint** — `POST /users/{userId}/publishing/test/{target}` that validates credentials without persisting them. FTP: connect + authenticate + list directory. SoundCloud: verify OAuth token + return quota.
- **Frontend: user-level credentials page** — configure and test FTP and SoundCloud credentials.
- **Frontend: per-podcast publication targets section** — enable/disable targets per podcast, configure target-specific settings (FTP remote path + public URL, SoundCloud playlist ID). Targets without user credentials are greyed out.
- **Migration** of existing `soundcloudPlaylistId` data from `podcasts` column into the new `podcast_publication_targets` table.

## Capabilities

### New Capabilities
- `publication-targets`: Per-podcast publication target configuration (table, CRUD API, enabled/disabled state) and migration from hardcoded SoundCloud fields.
- `ftp-publishing`: FTP(S) episode publisher implementation — upload MP3, sources.md, feed.xml, and podcast image to configurable remote path.
- `publishing-credentials`: User-level publication credential management (store, test, delete) for FTP and SoundCloud via `user_provider_configs`.
- `podcast-image`: Podcast artwork upload, validation, and storage.
- `episode-sources-file`: Per-episode `sources.md` generation and storage.
- `frontend-publication-settings`: Frontend UI for user-level credential management and per-podcast target configuration.

### Modified Capabilities
- `episode-publishing`: Publisher interface gains per-podcast target config awareness; `soundcloudPlaylistId` moves off the `Podcast` entity. FTP supports republishing (update).
- `soundcloud-integration`: SoundCloud `clientId`/`clientSecret`/`callbackUri` move from `AppProperties` env vars to `user_provider_configs` (with env var fallback). OAuth and publish flows read credentials from DB.
- `user-api-keys`: `PUBLISHING` added as a valid category. API key value becomes a JSON blob for multi-field credentials.
- `podcast-feed`: Feed title simplified, `<image>` tag added, episode descriptions changed to recap + sources.txt link. iTunes metadata (owner, author) added. `pubDate`, `lastBuildDate`, `ttl` added.
- `static-feed-export`: Static feed export uses `publicUrl` + `remotePath` from FTP target config when available.
- `data-directory`: Restructured from `./data/episodes/{podcastId}/` to `./data/{podcastId}/episodes/` for cleaner per-podcast layout.
- `audio-concatenation`: Fixed silence generator sample rate (44100→48000 Hz) to match TTS output and prevent broken MP3 files.

## Impact

- **Database**: New `podcast_publication_targets` table. Migration to move `soundcloudPlaylistId` data and drop column. New rows in `user_provider_configs` for publishing credentials. Existing episode show notes regenerated.
- **Backend**: New FTP publisher bean (uploads MP3 + sources.md + feed.xml + image), test connection endpoint, podcast image upload endpoint, sources.md generator, updated feed generator (simplified title, image tag, recap-only descriptions), updated SoundCloud credential resolution, updated `PublishingService` to check podcast-level target config.
- **Frontend**: New publication credentials settings page, new per-podcast targets configuration section, podcast image upload.
- **Config**: `APP_SOUNDCLOUD_CLIENT_ID` / `APP_SOUNDCLOUD_CLIENT_SECRET` env vars become fallback rather than primary source.

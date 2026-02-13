## Context

Episodes are currently uploaded to SoundCloud as standalone tracks. There is no grouping — each track exists independently on the user's profile. SoundCloud supports "playlists" (also called "sets") which can group tracks together, and the API provides endpoints to create playlists and add tracks to them.

The `SoundCloudPublisher` currently uploads a track and returns. The `Podcast` entity has no awareness of SoundCloud. The `SoundCloudClient` only has token exchange and track upload methods.

## Goals / Non-Goals

**Goals:**
- Automatically group published episodes into a SoundCloud playlist per podcast.
- Create the playlist on first publish, reuse it on subsequent publishes.
- Store the playlist mapping so it survives app restarts.

**Non-Goals:**
- Managing playlist metadata (artwork, descriptions) beyond initial creation.
- Handling playlist deletion on SoundCloud (if a user deletes the playlist manually, the next publish will create a new one).
- Publishing to playlists on platforms other than SoundCloud.

## Decisions

### Store playlist ID on the podcasts table

**Decision:** Add a nullable `soundcloud_playlist_id` column to the `podcasts` table.

**Rationale:** Podcasts already belong to a single user and map 1:1 to a SoundCloud playlist. Storing it on the podcast entity is the simplest approach — no new tables, no lookups. The column is nullable because not every podcast publishes to SoundCloud.

**Alternative considered:** A separate `podcast_platform_mappings` table. Rejected as over-engineering — there's only one platform (SoundCloud), and the mapping is 1:1.

### Create playlist on first publish, add tracks on subsequent publishes

**Decision:** The `SoundCloudPublisher.publish()` method checks if the podcast has a `soundcloudPlaylistId`. If not, it creates a new playlist via the API containing the just-uploaded track and stores the ID. If yes, it adds the track to the existing playlist.

**Rationale:** Fully automatic — no user setup required. The playlist is created lazily when actually needed.

**Alternative considered:** Require users to create/configure a playlist upfront. Rejected as unnecessary friction.

### Handle stale playlist IDs gracefully

**Decision:** If adding a track to a playlist fails with a 404 (playlist was deleted on SoundCloud), create a new playlist and update the stored ID. The track upload itself is not affected — only the playlist association.

**Rationale:** Users may delete playlists on SoundCloud. The publish should not fail entirely because of a missing playlist — the track is already uploaded successfully.

## Risks / Trade-offs

- **Extra API call per publish** → Each publish now makes one additional API call (create or update playlist). Acceptable latency trade-off for the grouping benefit.
- **Playlist ID stored on podcast entity** → Couples the podcast model slightly to SoundCloud. Acceptable given the single-platform scope.
- **Race condition on first publish** → If two episodes are published simultaneously for the same podcast, both may try to create a playlist. Mitigated by SQLite's single-writer lock — the second write will see the first's playlist ID after the transaction commits. The worst case is a duplicate playlist, which is harmless.

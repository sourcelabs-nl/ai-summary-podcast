## Context

The `EpisodePublisher` interface already has an `unpublish(userId, externalId)` method with a default that throws `UnsupportedOperationException`. `SoundCloudPublisher` implements it (calls `deleteTrack()`). `FtpPublisher` does not. The `PublishingService` uses `unpublish()` internally during same-day republication but never exposes it to the API.

## Goals / Non-Goals

**Goals:**
- Show episode date and day in the publications tab
- Allow users to unpublish from SoundCloud (delete track, rebuild playlist)
- Allow users to unpublish from FTP (delete MP3, regenerate feed.xml)
- Track unpublished state (`UNPUBLISHED` status) for history

**Non-Goals:**
- Bulk unpublish
- Undo/re-publish after unpublish (user can just publish again)

## Decisions

### 1. Unpublish endpoint: DELETE verb

**Decision:** `DELETE /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/publications/{target}`

**Why:** Semantically correct — we're removing a publication. Returns 200 with the updated publication record (status=UNPUBLISHED).

### 2. UNPUBLISHED status instead of record deletion

**Decision:** Add `UNPUBLISHED` to `PublicationStatus` enum. On unpublish, update the record status rather than deleting it.

**Why:** Preserves history. The user can see that an episode was once published and later unpublished. The unique constraint on `(episode_id, target)` means re-publishing after unpublish will update the existing record back to PUBLISHED.

### 3. FTP unpublish: delete MP3 + regenerate feed

**Decision:** `FtpPublisher.unpublish()` will delete the episode MP3 file from the remote FTP path, then call `postPublish()` to regenerate and upload `feed.xml`.

**Why:** Mirrors the publish flow in reverse. Feed regeneration ensures the RSS feed no longer references the deleted episode.

### 4. SoundCloud unpublish: delete track + rebuild playlist

**Decision:** After calling the existing `SoundCloudPublisher.unpublish()` (which deletes the track), also rebuild the playlist to remove the deleted track.

**Why:** Deleting a track doesn't automatically remove it from the playlist. The `rebuildPlaylist()` method already handles this.

### 5. Episode date in publications tab

**Decision:** Add "Date" and "Day" columns to the publications table, showing the episode's `generatedAt` date. The episode data is already available since publications are loaded per-episode.

## Risks / Trade-offs

**[Risk] FTP file deletion fails** → Log warning but still update status to UNPUBLISHED. The publication is conceptually unpublished even if cleanup fails.

**[Risk] SoundCloud track already deleted** → SoundCloud API may return 404. Treat as success (track is already gone) and update status.

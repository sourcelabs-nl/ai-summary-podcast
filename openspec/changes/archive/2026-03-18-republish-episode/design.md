## Context

The publish endpoint (`POST .../publish/{target}`) blocks with HTTP 409 when an episode is already published to a target. The frontend has a "Republish" button that calls this same endpoint, but it always fails for already-published episodes. `SoundCloudClient.updateTrack()` already exists and can update track metadata (description, permalink).

## Goals / Non-Goals

**Goals:**
- Allow republishing an already-published episode to update its metadata on the external platform
- Reuse the existing publish endpoint — no new endpoints needed

**Non-Goals:**
- Re-uploading the audio file (only metadata update)
- Supporting republish for failed publications (those should retry via normal publish)

## Decisions

### Decision 1: Add `update` method to `EpisodePublisher` interface

**Choice**: Add `fun update(episode: Episode, podcast: Podcast, userId: String, externalId: String): PublishResult` to the interface with a default implementation that throws `UnsupportedOperationException`.

**Alternatives considered**:
- Reuse `publish()` with conditional logic: Mixes create and update concerns, harder to reason about.

**Rationale**: Clean separation. Publishers that support updates implement it; others fail explicitly. SoundCloud already has `updateTrack` on the client.

### Decision 2: Modify `PublishingService.publish()` to branch on existing publication

**Choice**: When a `PUBLISHED` record exists, call `publisher.update()` instead of `publisher.publish()`, then update the publication record's `publishedAt` timestamp.

**Rationale**: Single endpoint, branching internally. The frontend "Republish" button already works — no frontend changes needed.

### Decision 3: SoundCloud update sends description and title only

**Choice**: `SoundCloudPublisher.update()` calls `soundCloudClient.updateTrack()` with the new description (show notes fallback chain) and returns the existing external URL.

**Rationale**: The audio doesn't change on republish. Only metadata (description with show notes, title) needs updating.

## Risks / Trade-offs

- **[Trade-off] Default `update` throws**: Future publishers must explicitly implement update support. Acceptable — it's opt-in.
- **[Risk] SoundCloud rate limiting**: Rapid republishing could hit API limits. Low risk for single-episode updates.

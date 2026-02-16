## Context

The `SoundCloudClient.addTrackToPlaylist` method sends a PUT request to `/playlists/{id}` with only the new track IDs. SoundCloud's PUT endpoint replaces the entire playlist track list, so all previously added tracks are lost. The fix requires fetching the current playlist state before updating.

## Goals / Non-Goals

**Goals:**
- Preserve existing tracks when appending a new track to a SoundCloud playlist
- Add a `getPlaylist` method to `SoundCloudClient` for fetching current playlist state

**Non-Goals:**
- Changing the playlist creation flow (works correctly)
- Handling SoundCloud API pagination for very large playlists (not a concern for podcast episodes)
- Migrating to SoundCloud's `urn` field (separate concern)

## Decisions

### Fetch-then-update approach
Fetch existing track IDs via `GET /playlists/{id}`, merge with new track ID, then PUT the full list. This is the only approach SoundCloud's API supports — there is no "append track" endpoint.

**Alternative considered:** SoundCloud API issue #352 discusses the inefficiency but no append endpoint exists.

### Fetch in SoundCloudPublisher, not SoundCloudClient
The merge logic (existing + new) belongs in `SoundCloudPublisher.addToPlaylist`, keeping `SoundCloudClient` as a thin API wrapper. `addTrackToPlaylist` continues to accept a full track list — it's the caller's responsibility to include existing tracks.

## Risks / Trade-offs

- **Race condition**: If two publishes happen simultaneously, one could overwrite the other's track. → Acceptable risk for a single-user podcast app with infrequent publishes.
- **Extra API call**: Each publish now makes an additional GET request. → Negligible latency for a background operation.
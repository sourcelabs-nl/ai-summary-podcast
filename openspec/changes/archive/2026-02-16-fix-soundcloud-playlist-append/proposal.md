## Why

When publishing a new episode to SoundCloud, the track is added to the playlist using a PUT request that sends only the new track ID. SoundCloud's PUT `/playlists/{id}` endpoint **replaces** the entire track list, causing all previously added tracks to be removed. The playlist should preserve existing tracks and append the new one.

## What Changes

- Add a `getPlaylist` method to `SoundCloudClient` that fetches an existing playlist's current track list via `GET /playlists/{id}`
- Update `SoundCloudPublisher.addToPlaylist` to fetch existing track IDs before updating, merging them with the new track ID
- Update `addTrackToPlaylist` in `SoundCloudClient` to accept the full (existing + new) track list

## Capabilities

### New Capabilities

_(none)_

### Modified Capabilities

- `soundcloud-integration`: The "SoundCloud add track to playlist" requirement must specify that existing tracks are fetched first and preserved when adding a new track via the PUT endpoint.

## Impact

- `SoundCloudClient.kt` — new `getPlaylist` method, updated `addTrackToPlaylist` usage
- `SoundCloudPublisher.kt` — `addToPlaylist` fetches existing tracks before PUT
- `SoundCloudPublisherTest.kt` — updated tests to verify track merging
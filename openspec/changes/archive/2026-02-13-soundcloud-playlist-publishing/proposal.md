## Why

Published episodes appear as isolated tracks on SoundCloud with no grouping. Users expect their podcast episodes to be collected in a playlist (set) so listeners can browse and play them as a series.

## What Changes

- After uploading a track to SoundCloud, the publisher automatically adds it to a playlist for that podcast.
- On first publish for a podcast, a new SoundCloud playlist is created via the API and its ID is stored.
- Subsequent publishes add the new track to the existing playlist.
- A new `soundcloud_playlist_id` column on the `podcasts` table stores the mapping.

## Capabilities

### New Capabilities

_None — this extends existing capabilities._

### Modified Capabilities

- `soundcloud-integration`: Add playlist creation and track-to-playlist management after upload.
- `episode-publishing`: Store the SoundCloud playlist ID per podcast and include it in the publish flow.

## Impact

- **Database**: New column `soundcloud_playlist_id` on the `podcasts` table (migration).
- **SoundCloud API**: Two new API calls — `POST /playlists` (create) and `PUT /playlists/{id}` (add track).
- **Code**: `SoundCloudClient`, `SoundCloudPublisher`, `Podcast` entity.
- **No breaking changes** — existing publish flow continues to work; playlist management is additive.

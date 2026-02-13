## 1. Database Migration

- [x] 1.1 Create Flyway migration `V15__add_soundcloud_playlist_id.sql` adding nullable `soundcloud_playlist_id TEXT` column to `podcasts` table
- [x] 1.2 Add `soundcloudPlaylistId` field to the `Podcast` data class

## 2. SoundCloud Client

- [x] 2.1 Add `createPlaylist(accessToken, title, trackIds): SoundCloudPlaylistResponse` method to `SoundCloudClient`
- [x] 2.2 Add `addTrackToPlaylist(accessToken, playlistId, trackIds): SoundCloudPlaylistResponse` method to `SoundCloudClient`
- [x] 2.3 Add `SoundCloudPlaylistResponse` data class with `id` and `permalinkUrl` fields

## 3. Publisher Integration

- [x] 3.1 Inject `PodcastRepository` into `SoundCloudPublisher`
- [x] 3.2 After track upload, check `podcast.soundcloudPlaylistId` — if null, call `createPlaylist` and save the ID on the podcast
- [x] 3.3 If playlist ID exists, call `addTrackToPlaylist` to add the new track
- [x] 3.4 Handle 404 on `addTrackToPlaylist` by creating a new playlist and updating the stored ID

## 4. Testing

- [x] 4.1 Write unit tests for `SoundCloudPublisher` playlist creation on first publish
- [x] 4.2 Write unit tests for `SoundCloudPublisher` adding track to existing playlist
- [x] 4.3 Write unit tests for `SoundCloudPublisher` stale playlist recreation on 404

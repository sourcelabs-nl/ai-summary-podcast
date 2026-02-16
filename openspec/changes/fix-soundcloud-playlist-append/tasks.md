## 1. SoundCloudClient changes

- [x] 1.1 Add `SoundCloudPlaylistDetailResponse` data class with `id` and `tracks` (list of objects with `id`) to `SoundCloudClient.kt`
- [x] 1.2 Add `getPlaylist(accessToken, playlistId)` method to `SoundCloudClient` that calls `GET /playlists/{playlistId}` and returns `SoundCloudPlaylistDetailResponse`

## 2. SoundCloudPublisher changes

- [x] 2.1 Update `addToPlaylist` in `SoundCloudPublisher` to call `getPlaylist` to fetch existing track IDs, merge with new track ID, and pass full list to `addTrackToPlaylist`
- [x] 2.2 Handle `HttpClientErrorException.NotFound` from `getPlaylist` by falling through to `createNewPlaylist`

## 3. Playlist rebuild admin endpoint

- [x] 3.1 Add `findPublishedByPodcastIdAndTarget(podcastId, target)` query to `EpisodePublicationRepository` joining `episode_publications` with `episodes`
- [x] 3.2 Add `rebuildPlaylist(podcast, userId, trackIds)` method to `SoundCloudPublisher` that replaces playlist contents with full track list
- [x] 3.3 Add `PlaylistController` with `POST /users/{userId}/podcasts/{podcastId}/playlist/rebuild` endpoint

## 4. Tests

- [x] 4.1 Update `subsequent publish adds track to existing playlist` test to mock `getPlaylist` returning existing tracks and verify `addTrackToPlaylist` receives merged list
- [x] 4.2 Update `stale playlist triggers recreation` test to handle 404 from `getPlaylist`
- [x] 4.3 Add `rebuildPlaylist` tests: existing playlist, no playlist, and 404 fallback
- [x] 4.4 Run `./mvnw test` and verify all tests pass

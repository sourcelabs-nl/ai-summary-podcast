## 1. SoundCloudClient changes

- [x] 1.1 Add `SoundCloudPlaylistDetailResponse` data class with `id` and `tracks` (list of objects with `id`) to `SoundCloudClient.kt`
- [x] 1.2 Add `getPlaylist(accessToken, playlistId)` method to `SoundCloudClient` that calls `GET /playlists/{playlistId}` and returns `SoundCloudPlaylistDetailResponse`
- [x] 1.3 Add `updateTrack(accessToken, trackId, permalink)` method to `SoundCloudClient` that calls `PUT /tracks/{trackId}` to update the track permalink

## 2. SoundCloudPublisher changes

- [x] 2.1 Update `addToPlaylist` in `SoundCloudPublisher` to call `getPlaylist` to fetch existing track IDs, merge with new track ID, and pass full list to `addTrackToPlaylist`
- [x] 2.2 Handle `HttpClientErrorException.NotFound` from `getPlaylist` by falling through to `createNewPlaylist`

## 3. Playlist rebuild admin endpoint

- [x] 3.1 Add `findPublishedByPodcastIdAndTarget(podcastId, target)` query to `EpisodePublicationRepository` joining `episode_publications` with `episodes`
- [x] 3.2 Add `rebuildPlaylist(podcast, userId, trackIds)` method to `SoundCloudPublisher` that replaces playlist contents with full track list
- [x] 3.3 Add `PlaylistController` with `POST /users/{userId}/podcasts/{podcastId}/playlist/rebuild` endpoint
- [x] 3.4 Add `updateTrackPermalinks(podcast, userId, episodes, publications)` to `SoundCloudPublisher` to update track permalinks during rebuild
- [x] 3.5 Add `permalink` field to `TrackUploadRequest` and set `track[permalink]` during upload
- [x] 3.6 Add `buildPermalink` to generate slug from podcast name + episode date

## 4. Tests

- [x] 4.1 Update `subsequent publish adds track to existing playlist` test to mock `getPlaylist` returning existing tracks and verify `addTrackToPlaylist` receives merged list
- [x] 4.2 Update `stale playlist triggers recreation` test to handle 404 from `getPlaylist`
- [x] 4.3 Add `rebuildPlaylist` tests: existing playlist, no playlist, and 404 fallback
- [x] 4.4 Add `updateTrackPermalinks` test verifying correct permalink per episode
- [x] 4.5 Add permalink assertion to `publish uploads track with correct metadata` test
- [x] 4.6 Run `./mvnw test` and verify all tests pass

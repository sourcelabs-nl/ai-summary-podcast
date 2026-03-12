## 1. Data Model & Migration

- [x] 1.1 Add `PUBLISHING` to `ApiKeyCategory` enum
- [x] 1.2 Create `PodcastPublicationTarget` entity and `PodcastPublicationTargetRepository` with `findByPodcastId`, `findByPodcastIdAndTarget`, `deleteByPodcastIdAndTarget`
- [x] 1.3 Create Flyway migration: `podcast_publication_targets` table with columns, constraints, and cascade delete
- [x] 1.4 Create Flyway migration: migrate `soundcloud_playlist_id` data from `podcasts` into `podcast_publication_targets`, then drop the column (SQLite table recreation)
- [x] 1.5 Remove `soundcloudPlaylistId` field from `Podcast` entity

## 2. Publication Targets CRUD API

- [x] 2.1 Create `PodcastPublicationTargetService` with list, upsert, and delete methods
- [x] 2.2 Create `PodcastPublicationTargetController` with GET/PUT/DELETE endpoints at `/users/{userId}/podcasts/{podcastId}/publication-targets/{target}`
- [x] 2.3 Write tests for publication target service and controller

## 3. Publishing Credentials & Test Connection

- [x] 3.1 Update `UserProviderConfigService` to skip base URL validation for `PUBLISHING` category
- [x] 3.2 Create `PublishingTestService` with `testFtp(credentials)` and `testSoundCloud(userId)` methods
- [x] 3.3 Create `PublishingTestController` with `POST /users/{userId}/publishing/test/{target}` endpoint
- [x] 3.4 Write tests for test connection endpoint

## 4. SoundCloud Credential Resolution Refactor

- [x] 4.1 Create `SoundCloudCredentialResolver` that checks `user_provider_configs` then falls back to `AppProperties`
- [x] 4.2 Update `SoundCloudOAuthController` to use `SoundCloudCredentialResolver` instead of `AppProperties` directly
- [x] 4.3 Update `SoundCloudClient` to accept resolved credentials (updated SoundCloudTokenManager to use resolver)
- [x] 4.4 Update `SoundCloudPublisher` to read/write playlist ID from `podcast_publication_targets` instead of `Podcast.soundcloudPlaylistId`
- [x] 4.5 Update `PublishingService.rebuildSoundCloudPlaylist` to use `podcast_publication_targets` for playlist ID (handled by SoundCloudPublisher refactor)
- [x] 4.6 Write tests for SoundCloud credential resolution and updated playlist ID handling

## 5. Podcast Image Upload

- [x] 5.1 Create `PodcastImageService` with upload (validate type + size), get, and delete methods
- [x] 5.2 Create `PodcastImageController` with POST/GET/DELETE endpoints at `/users/{userId}/podcasts/{podcastId}/image`
- [x] 5.3 Write tests for image upload validation (type, size, magic bytes)

## 6. Episode Sources File & Show Notes Refactor

- [x] 6.1 Create `EpisodeSourcesGenerator` that generates `sources.md` (podcast name, date, recap, formatted source list)
- [x] 6.2 Integrate sources.md generation into `EpisodeService.createEpisodeFromPipelineResult` pipeline
- [x] 6.3 Update `EpisodeService.buildShowNotes` to produce recap-only format (no inline sources)
- [x] 6.4 Create admin endpoint or startup task to regenerate show notes and sources.md for existing episodes
- [x] 6.5 Write tests for sources.md generation and show notes format

## 7. Feed XML Changes

- [x] 7.1 Update `FeedGenerator` title to use podcast name only
- [x] 7.2 Add `<image>` element to feed when podcast has a stored image
- [x] 7.3 Update episode `<description>` to use recap + sources.md link (when publicUrl available) instead of inline show notes
- [x] 7.4 Update `FeedGenerator.generate()` to accept optional `publicUrl` parameter for sources.md links and image URLs
- [x] 7.5 Update `StaticFeedExporter` to resolve `publicUrl` from FTP target config (priority: FTP publicUrl > static-base-url > base-url)
- [x] 7.6 Write tests for feed XML changes (title, image, description format)

## 8. FTP Publisher

- [x] 8.1 Add Apache Commons Net dependency to `pom.xml`
- [x] 8.2 Create `FtpPublisher` implementing `EpisodePublisher` with FTP/FTPS connection logic
- [x] 8.3 Implement multi-file upload: sources.md, MP3, feed.xml (regenerated with publicUrl), podcast image
- [x] 8.4 Write tests for FTP publisher (mock FTP client)

## 9. Publishing Service Target Validation

- [x] 9.1 Update `PublishingService.publish()` to verify podcast has enabled `podcast_publication_targets` entry for the target before publishing
- [x] 9.2 Update `PublishingController` error handling for target-not-configured case
- [x] 9.3 Write tests for target validation in publishing flow

## 10. Frontend: Publication Credentials Page

- [x] 10.1 Create publication credentials settings page with FTP and SoundCloud cards
- [x] 10.2 Implement FTP credential form (host, port, username, password, useTls) with save functionality
- [x] 10.3 Implement SoundCloud credential form (clientId, clientSecret, callbackUri) with OAuth status display
- [x] 10.4 Implement "Test Connection" button for both targets with result feedback
- [x] 10.5 Add navigation to publication credentials page from user settings

## 11. Frontend: Per-Podcast Publication Targets

- [x] 11.1 Create publication targets section in podcast settings page
- [x] 11.2 Implement target cards with enable/disable toggle and config fields (FTP: remotePath + publicUrl, SoundCloud: playlistId read-only)
- [x] 11.3 Grey out targets without user credentials with hint message
- [x] 11.4 Wire up save/load to publication targets API endpoints

## 12. Frontend: Podcast Image Upload

- [x] 12.1 Add image upload component to podcast settings (drag-and-drop or file picker)
- [x] 12.2 Show current image preview with delete option
- [x] 12.3 Client-side validation (image type, max 1MB) before upload

## 13. Data Directory Restructure

- [x] 13.1 Change data directory layout from `./data/episodes/{podcastId}/` to `./data/{podcastId}/episodes/`
- [x] 13.2 Update `EpisodesProperties` default directory from `./data/episodes` to `./data`
- [x] 13.3 Update `TtsPipeline`, `EpisodeSourcesGenerator`, `WebConfig` for new path structure
- [x] 13.4 Create Flyway migration V42 to update `audio_file_path` in database
- [x] 13.5 Update `FtpPublisher` upload structure: `{remotePath}/{podcastId}/episodes/` for audio+sources, `{remotePath}/{podcastId}/` for feed.xml+image

## 14. FTP Publishing Enhancements

- [x] 14.1 Add FTP as a target in the frontend publish wizard
- [x] 14.2 Implement `FtpPublisher.update()` to support republishing
- [x] 14.3 Allow publishing to new targets for already-published episodes
- [x] 14.4 Feed.xml only includes episodes published via the target (`publishedTarget` filter)
- [x] 14.5 Remote path uses user-provided value directly; defaults to `/{podcastId}/` when empty
- [x] 14.6 Public URL combined with remote path for correct URL construction (`{publicUrl}/{remotePath}/`)
- [x] 14.7 Show podcast UUID as placeholder for Remote Path field in settings UI

## 15. RSS Feed Metadata

- [x] 15.1 Add iTunes metadata (itunes:owner, itunes:author) to feed.xml via rome-modules
- [x] 15.2 Add `pubDate`, `lastBuildDate`, and `<ttl>60</ttl>` to feed.xml
- [x] 15.3 Configure owner-name, owner-email, author in application.yaml

## 16. Sources File & Audio Fixes

- [x] 16.1 Change sources file extension from `.md` to `.txt` for browser viewability
- [x] 16.2 Fix audio concatenation: silence generator sample rate from 44100 Hz to 48000 Hz to match TTS output
- [x] 16.3 Re-encode existing MP3 files to fix sample rate mismatch

## 17. Frontend: Publications Tab Enhancements

- [x] 17.1 Add Server icon next to FTP target label in publications tab
- [x] 17.2 Add Feed URL link next to Track link for FTP publications
- [x] 17.3 Move publications tab to last position
- [x] 17.4 Reduce save buttons on publishing settings tab from 3 to 2 (hide global save)

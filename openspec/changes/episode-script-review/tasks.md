## 1. Database Migration

- [x] 1.1 Create `V5__add_episode_review.sql` Flyway migration: recreate `episodes` table with nullable `audio_file_path` and `duration_seconds`, add `status TEXT NOT NULL DEFAULT 'GENERATED'` column, copy existing data with status `GENERATED`
- [x] 1.2 Add `require_review INTEGER NOT NULL DEFAULT 0` column to `podcasts` table in the same migration

## 2. Entity and Repository Changes

- [x] 2.1 Update `Episode` entity: add `status: String` field (default `GENERATED`), make `audioFilePath` and `durationSeconds` nullable (`String?` and `Int?`)
- [x] 2.2 Update `Podcast` entity and DTOs: add `requireReview: Boolean` field (default `false`), update `CreatePodcastRequest`, `UpdatePodcastRequest`, and `PodcastResponse` to include it
- [x] 2.3 Add `EpisodeRepository` queries: `findByPodcastIdAndStatusIn(podcastId, statuses)`, `findByPodcastIdAndStatus(podcastId, status)`
- [x] 2.4 Update `FeedGenerator` to only include episodes with status `GENERATED` (replace `findByPodcastId` with status-filtered query)

## 3. Pipeline Decoupling

- [x] 3.1 Update `BriefingGenerationScheduler.generateBriefing()`: when `podcast.requireReview` is true, save episode with status `PENDING_REVIEW` (script only, no TTS); skip generation if `PENDING_REVIEW` or `APPROVED` episode exists
- [x] 3.2 Update `PodcastController.generate()`: same branching logic for manual trigger — create pending episode when review required, return 409 if pending episode already exists

## 4. Async TTS Service

- [x] 4.1 Add `@EnableAsync` to `SchedulingConfig`
- [x] 4.2 Create `EpisodeService` with an `@Async` method that runs TTS pipeline, updates episode to `GENERATED` on success or `FAILED` on error

## 5. Episode Review Endpoints

- [x] 5.1 Create `EpisodeController` with `GET /users/{userId}/podcasts/{podcastId}/episodes` (list with optional `?status=` filter)
- [x] 5.2 Add `GET /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}` (get single episode)
- [x] 5.3 Add `PUT /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/script` (edit script, only when `PENDING_REVIEW`)
- [x] 5.4 Add `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/approve` (approve, triggers async TTS, accepts `PENDING_REVIEW` and `FAILED`)
- [x] 5.5 Add `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/discard` (discard, only when `PENDING_REVIEW`)

## 6. Tests

- [x] 6.1 Write tests for `EpisodeController`: list, get, edit, approve, discard — including status guard (409) scenarios
- [x] 6.2 Write tests for scheduler skip logic: verify generation is skipped when pending/approved episode exists
- [x] 6.3 Write tests for `EpisodeService` async TTS: verify status transitions to `GENERATED` on success and `FAILED` on error
- [x] 6.4 Write test for `FeedGenerator`: verify only `GENERATED` episodes appear in RSS feed

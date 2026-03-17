## 1. Pipeline Layer

- [x] 1.1 Add `recompose(articles, podcast)` method to `LlmPipeline` that runs composition-only (no scoring/aggregation), returning a `PipelineResult`
- [x] 1.2 Add `Article` import to `LlmPipeline.kt`

## 2. Service Layer

- [x] 2.1 Add `overrideGeneratedAt` and `updateLastGenerated` parameters to `EpisodeService.createEpisodeFromPipelineResult` with backward-compatible defaults
- [x] 2.2 Add `findById(episodeId)` convenience method to `EpisodeService`
- [x] 2.3 Add `EpisodeArticleRepository` dependency to `PodcastService` constructor
- [x] 2.4 Add `regenerateEpisode(sourceEpisode, podcast)` method to `PodcastService` that fetches linked articles, calls `recompose`, and creates a new episode with `overrideGeneratedAt` and `updateLastGenerated = false`

## 3. API Layer

- [x] 3.1 Add `POST /{podcastId}/episodes/{episodeId}/regenerate` endpoint to `PodcastController` with user/podcast/episode validation

## 4. Tests

- [x] 4.1 Fix `PodcastServiceTest` constructor to include new `EpisodeArticleRepository` mock parameter

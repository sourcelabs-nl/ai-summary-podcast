## 1. Database Schema & Entities

- [x] 1.1 Create Flyway migration to add `created_at TEXT NOT NULL DEFAULT '1970-01-01T00:00:00Z'` column to `sources` table
- [x] 1.2 Create Flyway migration to add `episode_articles` table with columns `id`, `episode_id`, `article_id` and unique constraint on `(episode_id, article_id)`
- [x] 1.3 Add `createdAt` field to `Source` entity
- [x] 1.4 Create `EpisodeArticle` entity and `EpisodeArticleRepository` with save and findByEpisodeId methods

## 2. Source Creation

- [x] 2.1 Update source creation in `SourceController` (or wherever sources are created) to set `createdAt = Instant.now().toString()`

## 3. Forward-Only Ingestion

- [x] 3.1 Update `SourcePoller.poll()` to skip posts with `publishedAt < source.createdAt` when `source.lastPolled == null`
- [x] 3.2 Add unit tests for forward-only ingestion: first poll skips old posts, first poll accepts new posts, subsequent polls ignore createdAt, null publishedAt is not filtered

## 4. Cross-Source Content Hash Dedup

- [x] 4.1 Add `findByContentHashAndSourceIdIn(contentHash, sourceIds)` method to `PostRepository`
- [x] 4.2 Update `SourcePollingScheduler` to resolve sibling source IDs (all sources in the same podcast) and pass them to `SourcePoller.poll()`
- [x] 4.3 Update `SourcePoller.poll()` to accept sibling source IDs and check cross-source dedup before saving a post
- [x] 4.4 Add unit tests for cross-source dedup: duplicate skipped within same podcast, same hash allowed across different podcasts

## 5. Episode-Article Tracking

- [x] 5.1 Add `processedArticleIds: List<Long>` field to `PipelineResult`
- [x] 5.2 Update `LlmPipeline.run()` to populate `processedArticleIds` from the articles marked as processed
- [x] 5.3 Update `BriefingGenerationScheduler.generateBriefing()` to save `EpisodeArticle` records after episode creation using the article IDs from `PipelineResult`
- [x] 5.4 Add unit tests for episode-article link creation in the scheduler

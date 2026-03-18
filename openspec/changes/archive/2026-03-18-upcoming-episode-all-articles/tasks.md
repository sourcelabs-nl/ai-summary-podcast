## 1. Repository Queries

- [x] 1.1 Add `findAllSince(sourceIds, since)` query to `ArticleRepository` — returns all articles (scored and unscored) with `published_at >= :since` for the given source IDs
- [x] 1.2 Add `findUnlinkedSince(sourceIds, since)` query to `PostRepository` — returns unlinked posts with `created_at >= :since` for the given source IDs

## 2. Service Layer

- [x] 2.1 Add `getUpcomingContent(podcast)` method to `PodcastService` (or a dedicated service) that computes the since-cutoff from `podcast.lastGeneratedAt` (fallback: `maxArticleAgeDays`), queries both articles and unlinked posts, and returns a merged list sorted by relevance score descending (nulls last)

## 3. Controller Refactor

- [x] 3.1 Update `PodcastController.upcomingArticles()` to delegate to the service method from task 2.1 — remove direct repository access and business logic from the controller

## 4. Frontend

- [x] 4.1 Update the podcast detail page (`frontend/src/app/podcasts/[podcastId]/page.tsx`) to always show the "Next Episode" link, displaying "no articles yet" when the count is 0

## 5. Testing

- [x] 5.1 Add unit test for the new service method covering: articles + posts merged, fallback when `lastGeneratedAt` is null, empty results
- [x] 5.2 Verify existing `ArticleRepositoryTest` still passes and add a test for the new `findAllSince` query

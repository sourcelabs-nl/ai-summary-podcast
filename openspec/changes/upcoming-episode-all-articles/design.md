## Context

The `upcoming-articles` endpoint in `PodcastController` currently calls `articleRepository.findRelevantUnprocessedBySourceIds()` which filters by `relevance_score >= threshold AND is_processed = 0`. This hides:
1. Articles scored below the threshold (scored but filtered out)
2. Unlinked posts not yet aggregated into articles (posts exist but no article yet)

The controller also directly accesses repositories (`ArticleRepository`, `PostRepository`, `SourceRepository`) instead of delegating to the service layer, violating the project's architecture guidelines.

The `Podcast` entity has a `lastGeneratedAt` field that tracks when the last episode was generated — this is the natural boundary for "since last episode".

## Goals / Non-Goals

**Goals:**
- Show all content (articles + unlinked posts) collected since the last episode generation
- Always display the "Next Episode" link on the podcast detail page
- Move business logic from PodcastController into a service

**Non-Goals:**
- Changing how the pipeline selects articles for episode generation (still uses relevance threshold)
- Changing the scoring or aggregation pipeline
- Adding new UI for filtering/sorting articles by score

## Decisions

### 1. Time-based filtering using `lastGeneratedAt`

Use `podcast.lastGeneratedAt` as the cutoff for "since last episode". Fall back to `maxArticleAgeDays` (default 7) if no episode has been generated yet.

**Rationale**: `is_processed` flag only captures articles that made it into an episode — below-threshold articles accumulate indefinitely. `lastGeneratedAt` is the true boundary.

**Alternative considered**: Using the most recent episode's `generatedAt` via `EpisodeRepository.findMostRecentByPodcastId()`. Rejected because `lastGeneratedAt` on the podcast is simpler and already maintained.

### 2. Include unlinked posts alongside articles

Query both `articles` (published_at >= since) and unlinked `posts` (created_at >= since) and merge them into a single response list. Posts are returned with `relevanceScore: null` and `summary: null`.

**Rationale**: Posts are the raw content that will become articles after aggregation. Without them, the upcoming view shows nothing between episodes until the pipeline runs.

### 3. Create a dedicated service method

Introduce an `UpcomingContentService` or add a method to `PodcastService` that encapsulates the "fetch upcoming content" logic, keeping the controller thin.

**Rationale**: Architecture guidelines require controllers to delegate to services. The current implementation has the controller directly querying repositories and merging results.

## Risks / Trade-offs

- **[Risk] Duplicate content**: A post and its aggregated article could both appear if aggregation happens between polling and viewing. → **Mitigation**: The time window (since last episode) naturally limits this; aggregated articles replace their posts, and the `post_articles` join table excludes linked posts.
- **[Risk] Large result sets**: Podcasts with many sources and long gaps between episodes could return many items. → **Mitigation**: Acceptable for now; pagination can be added later if needed.

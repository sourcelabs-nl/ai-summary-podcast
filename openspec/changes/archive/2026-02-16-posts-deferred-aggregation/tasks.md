## 1. Database Schema

- [x] 1.1 Create Flyway migration V16__add_posts_table.sql — adds `posts` table (id, source_id, title, body, url, published_at, author, content_hash, created_at), unique constraint on (source_id, content_hash), index on (source_id, created_at)
- [x] 1.2 Add `post_articles` join table to V16 migration — (id, post_id FK, article_id FK), unique constraint on (post_id, article_id)

## 2. Post Entity and Repository

- [x] 2.1 Create `Post` data class (store package) mapped to the `posts` table with all columns
- [x] 2.2 Create `PostArticle` data class mapped to the `post_articles` table
- [x] 2.3 Create `PostRepository` with Spring Data JDBC — methods: save, findBySourceIdAndContentHash (dedup), findUnlinkedBySourceIds (posts with no post_articles entry within time window)
- [x] 2.4 Create `PostArticleRepository` with Spring Data JDBC — methods: save, findByArticleId
- [x] 2.5 Write tests for PostRepository (dedup, unlinked queries, time window filtering)

## 3. Source Pollers — Write to Posts

- [x] 3.1 Refactor `SourcePoller` to write to `PostRepository` instead of `ArticleRepository` — map fetched items to `Post` entities, compute content hash, handle dedup
- [x] 3.2 Remove `SourceAggregator` call from polling pipeline — pollers no longer aggregate at poll time
- [x] 3.3 Update `RssFeedFetcher` return type and callers to work with `Post` instead of `Article`
- [x] 3.4 Update `TwitterFetcher` return type and callers to work with `Post` instead of `Article`
- [x] 3.5 Update `WebsiteFetcher` return type and callers to work with `Post` instead of `Article`
- [x] 3.6 Update existing source poller tests to verify posts are written instead of articles

## 4. Deferred Aggregation

- [x] 4.1 Refactor `SourceAggregator` to accept `Post` entities instead of `Article` — output is `Article` entities with `post_articles` linkage
- [x] 4.2 Add time-window parameter to aggregation — default to `app.source.max-article-age-days`
- [x] 4.3 For non-aggregated sources, create 1:1 post→article mapping with `post_articles` entry
- [x] 4.4 Persist created articles and post_articles join entries from the aggregator
- [x] 4.5 Write tests for deferred aggregation (aggregated, non-aggregated, time window, join table entries)

## 5. LLM Pipeline — Two-Stage

- [x] 5.1 Create `ArticleScoreSummarizer` component — single LLM call that returns structured JSON (relevanceScore, summary, includedPostIds, excludedPostIds)
- [x] 5.2 Define the structured output schema/prompt for score+summarize+filter
- [x] 5.3 Refactor `LlmPipeline.run()` — insert aggregation step before LLM stages, replace three stages with two (score+summarize+filter → compose)
- [x] 5.4 Remove `RelevanceScorer` and `ArticleSummarizer` components (replaced by `ArticleScoreSummarizer`)
- [x] 5.5 Update `BriefingComposer` if needed — ensure it works with the new article structure
- [x] 5.6 Write tests for `ArticleScoreSummarizer` (relevant, irrelevant, mixed, attribution preservation)
- [x] 5.7 Write tests for the refactored `LlmPipeline` (end-to-end with aggregation step)

## 6. Cleanup

- [x] 6.1 Update old unprocessed article cleanup to also clean up old unlinked posts (posts with no post_articles entry older than max-article-age-days)
- [x] 6.2 Write tests for post cleanup logic
- [x] 6.3 Remove any dead code from the old three-stage pipeline

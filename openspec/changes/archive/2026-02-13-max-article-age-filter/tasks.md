## 1. Configuration

- [x] 1.1 Add `SourceProperties` data class with `maxArticleAgeDays: Int = 7` to `AppProperties.kt`
- [x] 1.2 Add `app.source.max-article-age-days: 7` default to `application.yml`

## 2. Core Implementation

- [x] 2.1 Add age filter in `SourcePoller.poll()` — skip articles where `publishedAt` is older than `maxArticleAgeDays`, allow null `publishedAt` through
- [x] 2.2 Add `deleteOldUnprocessedArticles(cutoff)` query to `ArticleRepository` — delete articles where `published_at < cutoff AND is_processed = 0`
- [x] 2.3 Call cleanup from `SourcePollingScheduler` before polling sources

## 3. Tests

- [x] 3.1 Add unit tests for `SourcePoller` age filtering: article within age saved, article older than max age skipped, article with null `publishedAt` saved
- [x] 3.2 Add unit test for old article cleanup in `SourcePollingScheduler`

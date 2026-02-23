## 1. Repository Changes

- [x] 1.1 Add `deleteByArticleId(articleId: Long)` method to `PostArticleRepository`
- [x] 1.2 Add `countByArticleId(articleId: Long)` method to `PostArticleRepository` to detect aggregated articles

## 2. Core Implementation

- [x] 2.1 Update `EpisodeService.discardAndResetArticles()` to detect aggregated articles (2+ post-article links) and handle them differently: delete post-article links and the article itself, instead of resetting `isProcessed`

## 3. Tests

- [x] 3.1 Add unit test: discard with non-aggregated articles resets `isProcessed` (existing behavior preserved)
- [x] 3.2 Add unit test: discard with aggregated article deletes post-article links and the article
- [x] 3.3 Add unit test: discard with mixed article types handles each correctly

## Why

When an episode is discarded, all linked articles are reset (`isProcessed = false`) so they can be reused in a future episode. For aggregated articles (X/Nitter posts combined into one article), this means the stale aggregated article gets reused as-is. If new posts arrived since, the pipeline creates a second aggregated article alongside the old one, leading to overlapping content in the next episode. Aggregated articles should instead be deleted and their posts unlinked, so all posts (old + new) get re-aggregated into a single fresh article on the next pipeline run.

## What Changes

- For **non-aggregated** articles (RSS, YouTube, etc.): no change — reset `isProcessed = false` as today.
- For **aggregated** articles (linked to multiple posts via `post_articles`): delete the article and remove `post_articles` links, so the posts become unlinked and eligible for re-aggregation on the next pipeline run.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `episode-review`: The discard behavior changes for aggregated articles — instead of resetting `isProcessed`, the system deletes the article and unlinks its posts.

## Impact

- `EpisodeService.discardAndResetArticles()` — needs to detect aggregated articles and handle them differently.
- `PostArticleRepository` — may need a new query to find post-article links by article ID and delete them.
- `ArticleRepository` — needs to delete individual articles by ID.

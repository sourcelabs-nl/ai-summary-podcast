## Why

When a user discards a `PENDING_REVIEW` episode, the linked articles remain marked as `isProcessed = true`. This means those articles are permanently excluded from future episode generation — they never appear in any subsequent episode. The user loses that content with no way to recover it into the pipeline.

## What Changes

- When an episode is discarded, reset `isProcessed = false` on all articles linked to that episode via the `episode_articles` table, so they become eligible for the next generation cycle.
- Update the episode-review spec to document this behavior as a requirement.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `episode-review`: The discard scenario shall also reset the `isProcessed` flag on linked articles so they are re-eligible for future episodes.

## Impact

- `EpisodeController.kt` — discard endpoint gains article reset logic
- `ArticleRepository.kt` — may need a bulk update method for resetting `isProcessed`
- `EpisodeArticleRepository.kt` — already has `findByEpisodeId`, used to look up linked articles
- No API contract changes (same endpoint, same request/response)
- No database schema changes (`isProcessed` column already exists)
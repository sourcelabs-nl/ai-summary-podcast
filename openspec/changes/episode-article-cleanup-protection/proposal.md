## Why

The article cleanup query (`deleteOldUnprocessedArticles`) relies solely on the `is_processed` flag to protect articles that contributed to episodes. If `is_processed` were ever incorrectly set or reset, articles linked to published episodes could be deleted. Now that the `episode_articles` join table exists, the cleanup queries should also check it as a belt-and-suspenders safeguard — an article linked to any episode must never be deleted regardless of its `is_processed` state.

## What Changes

- **Article cleanup query**: Add an exclusion for articles that have entries in the `episode_articles` table, so linked articles are protected even if `is_processed = false`.
- **Post cleanup query**: Add an exclusion for posts whose articles are linked to episodes via `episode_articles`, so the underlying posts of episode-linked articles are also protected.

## Capabilities

### New Capabilities

_(none)_

### Modified Capabilities

- `content-store`: Update the `deleteOldUnprocessedArticles` requirement to exclude articles linked to episodes via `episode_articles`.
- `post-store`: Update the `deleteOldUnlinkedPosts` requirement to exclude posts whose articles are linked to episodes via `episode_articles`.

## Impact

- **Database**: Two existing cleanup queries gain an additional `NOT IN (SELECT ...)` subquery against `episode_articles`.
- **Existing data**: No migration needed — this is a query-only change that makes cleanup more conservative.
- **Risk**: Minimal — the change only prevents deletions, never causes them.

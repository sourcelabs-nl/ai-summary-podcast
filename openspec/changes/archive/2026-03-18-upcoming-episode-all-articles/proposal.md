## Why

The upcoming episode page only shows articles that scored above the relevance threshold, hiding low-scoring and unscored content. Users need to see all collected content since the last episode — regardless of score — to evaluate relevance scoring accuracy and tweak thresholds. Additionally, unlinked posts (not yet aggregated into articles) are invisible in the current view.

## What Changes

- The `upcoming-articles` API endpoint returns all articles and unlinked posts published since the last episode generation, instead of only above-threshold scored articles
- The frontend "Next Episode" link is always visible on the podcast detail page, even when no articles are available
- The controller logic is refactored to delegate to the service layer (currently bypasses it by calling repositories directly)

## Capabilities

### New Capabilities

_(none)_

### Modified Capabilities

- `upcoming-articles-api`: Change filtering from "relevant unprocessed" (score >= threshold) to "all content since last episode" (articles + unlinked posts since `lastGeneratedAt`)
- `frontend-upcoming-episode`: Always show the "Next Episode" link on the podcast detail page; display unscored posts alongside scored articles

## Impact

- **Backend**: `ArticleRepository` (new query), `PostRepository` (new query), `PodcastController` (endpoint change + service layer refactor)
- **Frontend**: Podcast detail page (`page.tsx`) — conditional rendering removed for upcoming link
- **API contract**: Response now includes unscored items (relevanceScore: null) and items below the threshold

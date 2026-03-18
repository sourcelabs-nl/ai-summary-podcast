## Why

Listeners reported getting distracted during podcast episodes. To improve engagement, the briefing script needs techniques like hook openings, curiosity loops, mid-roll callbacks, and shorter segments. To A/B test these changes without affecting the regular pipeline, an episode regeneration feature is needed that re-composes an existing episode's script using its original articles but with updated podcast settings (e.g. new `customInstructions`).

## What Changes

- Add an episode regeneration endpoint that re-runs script composition for an existing episode using its original linked articles and current podcast settings
- The regenerated episode is saved as a new episode, preserving the original episode untouched
- Regeneration inherits the source episode's `generatedAt` timestamp (not the current time)
- Regeneration does not update the podcast's `lastGeneratedAt`, preventing article window drift
- Add `recompose()` method to `LlmPipeline` that performs composition-only (skips scoring/aggregation)
- Add `overrideGeneratedAt` and `updateLastGenerated` parameters to `EpisodeService.createEpisodeFromPipelineResult` for reuse by the regeneration flow

## Capabilities

### New Capabilities
- `episode-regeneration`: Re-compose an existing episode's script using its original articles and current podcast settings, creating a new episode without affecting the regular generation pipeline

### Modified Capabilities
- `episode-review`: `createEpisodeFromPipelineResult` now accepts optional `overrideGeneratedAt` and `updateLastGenerated` parameters to support regeneration without side effects

## Impact

- **API**: New endpoint `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/regenerate`
- **Backend**: `LlmPipeline.kt`, `PodcastService.kt`, `EpisodeService.kt`, `PodcastController.kt`
- **Tests**: `PodcastServiceTest.kt` updated for new `EpisodeArticleRepository` constructor parameter
- **No database migrations required** — uses existing `episode_articles` join table

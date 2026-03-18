## Why

The podcast detail page lacks real-time feedback during episode generation — users see no indication that ranking, scoring, or composing is happening. Additionally, the "Next Episode" card shows raw post counts instead of effective article counts (nitter posts that will be aggregated are counted individually), and there is no countdown to the next scheduled generation.

## What Changes

- Emit `pipeline.progress` SSE events during episode generation with stages: `aggregating`, `scoring`, `composing`
- Show inline generation progress on the podcast detail page "Next Episode" card (spinner + stage label, highlighted border)
- Add a live countdown timer to next scheduled generation (parsed from cron expression)
- Pre-calculate effective article count after aggregation in the `upcoming-articles` API (aggregatable source posts grouped as 1 article)
- Display both article and post counts: "47 articles / 91 posts ready"
- Hide the publish button when an episode is published to all targets (not just any target)

## Capabilities

### New Capabilities
- `pipeline-progress`: Real-time SSE events emitted during the LLM pipeline stages, surfaced as inline progress on the podcast detail page

### Modified Capabilities
- `upcoming-articles-api`: `articleCount` now returns the effective count after pre-calculating post-to-article aggregation
- `frontend-dashboard`: "Next Episode" card shows countdown timer, article/post counts, and inline pipeline progress
- `episode-publishing`: Publish button hidden when episode is published to all targets

## Impact

- **Backend**: `LlmPipeline.run()` gains `onProgress` callback; `PodcastService` emits `PodcastEvent`s for pipeline stages; `PodcastController` injects `SourceAggregator` for effective article count calculation
- **Frontend**: Podcast detail page subscribes to `pipeline.progress` events; new state for countdown timer and pipeline stage; `TARGETS` exported from publish-wizard
- **Tests**: `PodcastControllerTest`, `PodcastControllerLanguageTest`, `PodcastServiceTest` updated with new dependency mocks

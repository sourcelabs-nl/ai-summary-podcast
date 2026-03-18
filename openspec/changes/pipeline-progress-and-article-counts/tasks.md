## 1. Backend: Pipeline progress events

- [x] 1.1 Add `onProgress` callback parameter to `LlmPipeline.run()` with stages: aggregating, scoring, composing
- [x] 1.2 Inject `ApplicationEventPublisher` into `PodcastService` and emit `pipeline.progress` PodcastEvents from the callback
- [x] 1.3 Fix `PodcastServiceTest` to include the new `eventPublisher` dependency

## 2. Backend: Effective article count

- [x] 2.1 Inject `SourceAggregator` into `PodcastController` and compute effective `articleCount` by grouping unlinked posts from aggregatable sources
- [x] 2.2 Fix `PodcastControllerTest` and `PodcastControllerLanguageTest` to mock the new `SourceAggregator` dependency

## 3. Frontend: Pipeline progress display

- [x] 3.1 Add `pipeline.progress` to toast events in `event-context.tsx` with stage-specific messages
- [x] 3.2 Add `pipelineStage` state to podcast detail page and subscribe to `pipeline.progress` / `episode.created` / `episode.generated` events
- [x] 3.3 Update "Next Episode" card to show spinner + stage label with highlighted border during generation

## 4. Frontend: Countdown timer and article/post counts

- [x] 4.1 Add countdown timer using `cron-parser` that updates every second, hidden during pipeline progress
- [x] 4.2 Use `articleCount` from API response instead of `articles.length` for the article count
- [x] 4.3 Show "N articles / M posts ready" format when postCount > articleCount

## 5. Frontend: Fully published episode handling

- [x] 5.1 Export `TARGETS` from `publish-wizard.tsx` and import in podcast detail page
- [x] 5.2 Add `fullyPublishedEpisodeIds` state tracking episodes published to all targets
- [x] 5.3 Hide publish button when episode is in `fullyPublishedEpisodeIds`

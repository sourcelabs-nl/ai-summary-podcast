## 1. Configuration

- [x] 1.1 Add optional `staticBaseUrl` property to `FeedProperties` (nullable, defaults to null)

## 2. Core Implementation

- [x] 2.1 Add a `generate` overload to `FeedGenerator` (or a `baseUrl` parameter) that accepts a custom base URL for enclosure URLs, falling back to `app.feed.base-url` when null
- [x] 2.2 Create `StaticFeedExporter` component that calls `FeedGenerator` with the static base URL and writes the result to `data/episodes/{podcastId}/feed.xml`, logging a warning on failure

## 3. Integration

- [x] 3.1 Call `StaticFeedExporter` after episode generation in `TtsPipeline`
- [x] 3.2 Call `StaticFeedExporter` after episode approval + TTS in `EpisodeService` (covered by TtsPipeline.generateForExistingEpisode)
- [x] 3.3 Call `StaticFeedExporter` after episode cleanup in `EpisodeCleanup`

## 4. Tests

- [x] 4.1 Unit test `StaticFeedExporter`: verifies `feed.xml` is written with correct content
- [x] 4.2 Unit test `StaticFeedExporter`: verifies custom `staticBaseUrl` is used in enclosure URLs
- [x] 4.3 Unit test `StaticFeedExporter`: verifies write failure is logged as warning without throwing

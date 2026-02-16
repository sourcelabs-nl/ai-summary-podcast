## Why

The podcast create (`POST`) and update (`PUT`) endpoints silently ignore `relevanceThreshold` and `requireReview` values sent in JSON request bodies. The `PodcastService.create()` and `PodcastService.update()` methods do not copy these fields from the input `Podcast` object, causing them to always use entity defaults (5 and false respectively). This means users cannot configure relevance filtering or enable the review workflow via the API.

## What Changes

- Fix `PodcastService.create()` and `PodcastService.update()` to include `relevanceThreshold` and `requireReview` in the Podcast copy operations.
- Add defensive `@JsonProperty` annotations on nullable primitive DTO fields as a safeguard against Jackson 3 Kotlin module edge cases.
- Add controller-level tests that verify these fields are correctly deserialized and persisted for both create and update endpoints.

## Capabilities

### New Capabilities

_None — this is a bug fix._

### Modified Capabilities

- `podcast-management`: The update podcast requirement (scenario "Enable review on existing podcast") and create podcast scenarios involving `requireReview` are not working as specified. The fix restores correct behavior.
- `podcast-customization`: The relevance threshold requirement (scenario "Relevance threshold updated via API") is not working as specified. The fix restores correct behavior.

## Impact

- **Code**: `PodcastService.kt` — `create()` and `update()` methods; `PodcastController.kt` — defensive `@JsonProperty` annotations on DTOs
- **Tests**: New `PodcastControllerTest.kt` covering nullable field deserialization and persistence
- **APIs**: `POST /users/{userId}/podcasts` and `PUT /users/{userId}/podcasts/{podcastId}` will correctly accept `relevanceThreshold` and `requireReview`
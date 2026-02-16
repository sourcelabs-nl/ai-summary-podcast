## 1. Fix DTO deserialization

- [x] 1.1 Add `@JsonProperty` annotations to all nullable primitive fields (`Int?`, `Boolean?`, `Double?`) in `CreatePodcastRequest` in `PodcastController.kt`
- [x] 1.2 Add `@JsonProperty` annotations to all nullable primitive fields (`Int?`, `Boolean?`, `Double?`) in `UpdatePodcastRequest` in `PodcastController.kt`

## 2. Test coverage

- [x] 2.1 Create `PodcastControllerTest.kt` with a test that creates a podcast with `relevanceThreshold: 3` and `requireReview: true` and verifies the response contains the correct values
- [x] 2.2 Add a test that updates a podcast with `relevanceThreshold: 8` and `requireReview: true` and verifies the response contains the correct values
- [x] 2.3 Add a test that creates a podcast without optional fields and verifies defaults are applied (`relevanceThreshold: 5`, `requireReview: false`)

## 3. Verify

- [x] 3.1 Run the full test suite to confirm no regressions
- [x] 3.2 Restart the application and verify via curl that creating/updating a podcast with `relevanceThreshold` and `requireReview` works correctly

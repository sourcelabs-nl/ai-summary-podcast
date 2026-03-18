## 1. Publisher Interface

- [x] 1.1 Add `update(episode, podcast, userId, externalId)` method to `EpisodePublisher` interface with default `UnsupportedOperationException`

## 2. SoundCloud Publisher

- [x] 2.1 Implement `update()` in `SoundCloudPublisher` — call `soundCloudClient.updateTrack()` with updated description and title

## 3. Publishing Service

- [x] 3.1 Modify `PublishingService.publish()` to call `publisher.update()` when a `PUBLISHED` record already exists, instead of throwing 409
- [x] 3.2 Handle `UnsupportedOperationException` from publishers that don't support update — return appropriate error

## 4. Testing

- [x] 4.1 Test republish flow in `SoundCloudPublisherTest` — update called with correct description
- [x] 4.2 Update `PublishingService` tests for the republish path
- [x] 4.3 Verify all existing tests pass

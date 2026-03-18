## Why

The publish endpoint returns HTTP 409 when an episode is already published to a target. There is no way to update an existing publication (e.g., to push updated show notes or corrected metadata to SoundCloud). The frontend "Republish" button confirms the action but the backend rejects it.

## What Changes

- Allow re-publishing an already-published episode to the same target, updating the existing track instead of uploading a new one
- Add an `update` method to the `EpisodePublisher` interface for updating metadata on an existing publication
- SoundCloud publisher implements update by calling the track update API with the new description and title
- Publication record is updated with the new timestamp

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `episode-publishing`: Allow republishing to an already-published target by updating the existing external resource instead of blocking with 409

## Impact

- `PublishingService.publish()` — remove the 409 guard for already-published, add update path
- `EpisodePublisher` interface — add `update(episode, podcast, userId, externalId)` method
- `SoundCloudPublisher` — implement update using existing `updateTrack` client method
- `PublishingController` — no changes needed (same endpoint, different behavior)
- Frontend — no changes needed (republish button already calls the same endpoint)

## MODIFIED Requirements

### Requirement: Publish episode to target endpoint
The system SHALL provide a `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/publish/{target}` endpoint. When the episode has no existing publication for the target, the endpoint SHALL create a new publication as before. When the episode already has a `PUBLISHED` record for the target, the endpoint SHALL update the existing external resource's metadata and return HTTP 200 with the updated publication record.

#### Scenario: Republish updates existing publication
- **WHEN** a `POST .../publish/soundcloud` request is received for an episode that already has a `PUBLISHED` record for `"soundcloud"` with `externalId` `"12345"`
- **THEN** the system calls the publisher's update method with the external ID, updates the publication record's `publishedAt` timestamp, and returns HTTP 200 with the updated publication details

#### Scenario: Republish to SoundCloud updates track description
- **WHEN** a `POST .../publish/soundcloud` request is received for an already-published episode that now has show notes
- **THEN** the SoundCloud track description is updated to the episode's current show notes (or recap/script fallback)

### Requirement: Publisher update interface
The `EpisodePublisher` interface SHALL include an `update(episode, podcast, userId, externalId)` method that updates an already-published episode's metadata on the external platform. The default implementation SHALL throw `UnsupportedOperationException`. Publishers that support metadata updates SHALL override this method.

#### Scenario: Publisher supports update
- **WHEN** `SoundCloudPublisher.update()` is called with an episode and its external track ID
- **THEN** the track's description is updated on SoundCloud and a `PublishResult` is returned

#### Scenario: Publisher does not support update
- **WHEN** `update()` is called on a publisher that has not overridden the default
- **THEN** an `UnsupportedOperationException` is thrown and the endpoint returns HTTP 400

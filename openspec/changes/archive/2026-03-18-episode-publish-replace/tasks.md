## 1. Publisher Interface

- [x] 1.1 Add `unpublish(userId, externalId)` method to `EpisodePublisher` interface with default `UnsupportedOperationException`
- [x] 1.2 Implement `unpublish` in `SoundCloudPublisher` — delete track via `SoundCloudClient.deleteTrack`

## 2. Same-Day Replacement

- [x] 2.1 In `PublishingService.publish()`, after checking same-episode republish, find publications from other episodes with the same `generatedAt` date
- [x] 2.2 Unpublish old tracks and delete old publication records before publishing new episode
- [x] 2.3 Handle unpublish failures gracefully (log warning, still delete record and proceed)

## 3. Playlist Ordering

- [x] 3.1 In `PublishingService.rebuildSoundCloudPlaylist()`, sort publications by episode `generatedAt` descending (newest first) before building track ID list

## 4. Pronunciation Guide

- [x] 4.1 Strengthen TTS pronunciation guide instruction in `InworldTtsProvider.scriptGuidelines()` with "CRITICAL" emphasis to prevent LLMs from inventing IPA for unlisted words

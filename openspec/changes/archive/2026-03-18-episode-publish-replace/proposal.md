## Why

When publishing a regenerated episode to SoundCloud, the previous episode's track for the same day should be replaced rather than creating a duplicate. Additionally, the SoundCloud playlist rebuild should order tracks newest-first (standard podcast convention), and the TTS pronunciation guide needs stricter enforcement to prevent LLMs from inventing IPA for unlisted words.

## What Changes

- When publishing an episode to a target, automatically replace (unpublish + delete) any existing publication from another episode with the same `generatedAt` date
- Add `unpublish` method to `EpisodePublisher` interface, implemented by `SoundCloudPublisher` (deletes the track)
- Sort playlist tracks newest-first when rebuilding SoundCloud playlists
- Strengthen TTS pronunciation guide instruction to prevent LLMs from inventing IPA for words not in the pronunciation dictionary

## Capabilities

### New Capabilities

### Modified Capabilities
- `episode-publishing`: Publishing an episode now replaces same-day publications from other episodes (e.g. regenerated episodes). Playlist rebuild sorts newest-first.
- `pronunciation-dictionary`: Strengthened instruction to prevent LLMs from adding IPA for unlisted words.

## Impact

- **API**: No new endpoints; existing `POST .../publish/{target}` now handles same-day replacement
- **Backend**: `PublishingService.kt`, `EpisodePublisher.kt`, `SoundCloudPublisher.kt`, `InworldTtsProvider.kt`
- **No database migrations required**

# Delta: Episode Publishing

## ADDED Requirements

### Requirement: Same-day publication replacement
When publishing an episode to a target, the system SHALL check for existing publications from other episodes in the same podcast on the same target that share the same `generatedAt` date (YYYY-MM-DD). For each such publication, the system SHALL unpublish the old content from the external platform (e.g. delete the SoundCloud track) and delete the publication record before proceeding with the new publication.

#### Scenario: Publish regenerated episode replaces original
- **WHEN** episode 45 (generatedAt 2026-03-16) is published to SoundCloud, and episode 41 (generatedAt 2026-03-16) is already published to SoundCloud
- **THEN** the system deletes episode 41's SoundCloud track, removes its publication record, and publishes episode 45 as a new track

#### Scenario: Publish episode does not replace different-day episodes
- **WHEN** episode 45 (generatedAt 2026-03-16) is published to SoundCloud, and episode 32 (generatedAt 2026-03-05) is published to SoundCloud
- **THEN** episode 32's publication is not affected

#### Scenario: Unpublish failure does not block new publication
- **WHEN** unpublishing the old track fails (e.g. already deleted on SoundCloud)
- **THEN** the system logs a warning, deletes the old publication record, and proceeds with publishing the new episode

### Requirement: Unpublish support on publisher interface
The `EpisodePublisher` interface SHALL provide an `unpublish(userId, externalId)` method with a default implementation that throws `UnsupportedOperationException`. Publishers that support unpublishing (e.g. SoundCloud) SHALL override this method.

## MODIFIED Requirements

### Requirement: Playlist rebuild ordering
When rebuilding a SoundCloud playlist, tracks SHALL be ordered newest-first (descending by episode `generatedAt`). This matches standard podcast convention where the latest episode appears at the top.

## Why

The publications tab only shows the episode number — without the episode date or day, it's hard to identify which episode a publication belongs to. Additionally, there is no way to unpublish an episode from SoundCloud or FTP; the unpublish infrastructure exists at the publisher level but is never exposed to the user.

## What Changes

- Add episode date and day columns to the publications tab table
- Add `UNPUBLISHED` status to `PublicationStatus` enum
- Add unpublish API endpoint (`DELETE /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/publications/{target}`)
- Implement `PublishingService.unpublish()` — delegates to publisher, sets status to `UNPUBLISHED`
- Implement `FtpPublisher.unpublish()` — delete MP3 from FTP server and regenerate feed.xml
- SoundCloud unpublish already implemented (deletes track) — wire it up and rebuild playlist
- Add unpublish button to publications tab UI for PUBLISHED rows
- Emit SSE event `episode.unpublished` after successful unpublish

## Capabilities

### New Capabilities

(none)

### Modified Capabilities
- `episode-publishing`: Add UNPUBLISHED status, unpublish endpoint, and FTP unpublish implementation
- `frontend-publish-wizard`: Add episode date/day columns and unpublish button to publications tab

## Impact

- **Backend**: `PublishingController` (new endpoint), `PublishingService` (new method), `FtpPublisher` (implement `unpublish()`), `PublicationStatus` enum (add `UNPUBLISHED`), SSE event for unpublish
- **Frontend**: `publications-tab.tsx` (new columns, unpublish button + confirmation dialog)
- **APIs**: New `DELETE /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/publications/{target}` endpoint

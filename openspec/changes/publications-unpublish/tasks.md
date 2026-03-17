## 1. Backend: Status and Unpublish Service

- [x] 1.1 Add `UNPUBLISHED` to `PublicationStatus` enum
- [x] 1.2 Add `PublishingService.unpublish()` method — looks up PUBLISHED publication, calls publisher.unpublish(), updates status to UNPUBLISHED, clears externalId, publishes SSE event
- [x] 1.3 Implement `FtpPublisher.unpublish()` — delete MP3 file from FTP, regenerate feed.xml via postPublish()
- [x] 1.4 After SoundCloud unpublish, rebuild playlist to exclude deleted track

## 2. Backend: Unpublish Endpoint

- [x] 2.1 Add `DELETE /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/publications/{target}` endpoint in PublishingController
- [x] 2.2 Add `episode.unpublished` to SSE toast events in frontend EventProvider

## 3. Frontend: Publications Tab Improvements

- [x] 3.1 Add Date and Day columns to publications tab table (from episode generatedAt)
- [x] 3.2 Add Unpublish button (destructive icon-only) for PUBLISHED rows
- [x] 3.3 Add unpublish confirmation dialog and API call (DELETE)
- [x] 3.4 Handle UNPUBLISHED status badge (secondary/grey variant)

## 4. Tests

- [x] 4.1 Unit test PublishingService.unpublish() — success, not found, unknown target
- [x] 4.2 Verify existing PublishingController and PublishingService tests pass

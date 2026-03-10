## Why

When SoundCloud returns a 401 during publishing (token expired or quota exceeded masquerading as 401), the UI shows a generic "Publishing failed: 401 Unauthorized" message with no actionable recovery. The user must manually navigate to re-authorize. Additionally, SoundCloud's free plan has an upload quota — when exceeded, uploads silently fail with 401 and there's no way to identify or resolve the issue from the UI.

## What Changes

- Backend returns distinct HTTP status codes for OAuth expiry (401) and quota exceeded (413)
- Frontend publish wizard shows contextual recovery actions: re-authorize button for OAuth failures, and "Remove oldest track" for quota exceeded
- Status endpoint enriched with SoundCloud account quota information
- New endpoint to delete a SoundCloud track by ID
- Quota check queries SoundCloud `/me` API before attempting upload
- Oldest track lookup queries SoundCloud `/me/tracks` API filtered to tracks matching the podcast name

## Capabilities

### New Capabilities

- SoundCloud upload quota detection and reporting
- Delete SoundCloud track by ID via `DELETE /users/{userId}/oauth/soundcloud/tracks/{trackId}`
- Quota info included in `GET /users/{userId}/oauth/soundcloud/status` response
- Publish wizard re-authorize button on OAuth failures
- Publish wizard "Remove oldest track" prompt on quota exceeded

### Modified Capabilities

- Publishing endpoint returns 401 with `code: "oauth_expired"` for auth failures
- Publishing endpoint returns 413 with `code: "quota_exceeded"` and oldest track info for quota exceeded

## Impact

- `SoundCloudClient.kt` — new `getMe()`, `getMyTracks()`, `deleteTrack()` methods and data classes
- `SoundCloudPublisher.kt` — quota check before upload, `SoundCloudQuotaExceededException`
- `PublishingController.kt` — catch handlers for quota and OAuth errors
- `SoundCloudOAuthController.kt` — enriched status endpoint, new delete track endpoint
- `publish-wizard.tsx` — OAuth re-authorize button, quota exceeded with track deletion flow

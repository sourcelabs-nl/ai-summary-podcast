## Context

SoundCloud's free plan limits upload time. When exceeded, the API returns 401 on track uploads — misleadingly identical to an actual auth failure. OAuth token expiry also produces 401. The frontend had no way to distinguish these cases or recover from either.

## Goals / Non-Goals

**Goals:**
- Detect quota exceeded before attempting upload via SoundCloud `/me` API
- Provide clear error messages distinguishing OAuth expiry from quota exhaustion
- Allow users to re-authorize directly from the error dialog
- Allow users to delete the oldest app-uploaded track to free quota, directly from the publish wizard
- Include quota info in the SoundCloud status endpoint

**Non-Goals:**
- Full track management UI (this is a targeted recovery flow only)
- Automatic track rotation or quota management policies
- Support for deleting tracks not uploaded by this application

## Decisions

- **Quota check before upload**: Call `GET /me` to read `quota.uploadSecondsLeft` before attempting upload. Avoids the misleading 401 from SoundCloud.
- **Oldest track from SoundCloud API, not DB**: Use `GET /me/tracks` to find the actual oldest track on the current account, filtered by podcast name prefix. This handles account switches correctly (DB may have stale track IDs from a previous account).
- **Track name filtering**: Only offer to delete tracks whose title starts with the podcast name (e.g., "The Daily Agentic AI Podcast - "). This prevents accidental deletion of unrelated tracks.
- **Delete via SoundCloud OAuth controller**: Track deletion is an account-level operation on SoundCloud, not tied to a specific episode. Endpoint lives at `DELETE /users/{userId}/oauth/soundcloud/tracks/{trackId}`.
- **404 on delete = success**: If SoundCloud returns 404 when deleting a track, treat it as already deleted.
- **HTTP 413 for quota exceeded**: Distinguishes from 401 (OAuth) and 409 (already published). Response includes the oldest deletable track info.
- **Exception carries oldest track data**: `SoundCloudQuotaExceededException` includes the oldest track from SoundCloud so the controller doesn't need to query SoundCloud again or access repositories directly.

## Risks / Trade-offs

- Extra API call (`/me`) on every publish to check quota — acceptable since publishing is infrequent
- Fetching all tracks via `/me/tracks` on quota failure — bounded by SoundCloud's 200-item limit per page

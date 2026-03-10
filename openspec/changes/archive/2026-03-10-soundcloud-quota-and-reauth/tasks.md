## 1. SoundCloud API Extensions

- [x] 1.1 Add `SoundCloudQuota`, `SoundCloudMeResponse`, `SoundCloudTrackListResponse`, `SoundCloudTrackSummary` data classes to `SoundCloudClient.kt`
- [x] 1.2 Add `getMe(accessToken)` method to `SoundCloudClient` calling `GET /me`
- [x] 1.3 Add `getMyTracks(accessToken, limit)` method to `SoundCloudClient` calling `GET /me/tracks`
- [x] 1.4 Add `deleteTrack(accessToken, trackId)` method to `SoundCloudClient` calling `DELETE /tracks/{trackId}`, treating 404 as success

## 2. Quota Check and Exception

- [x] 2.1 Add `SoundCloudQuotaExceededException` with `oldestTrack: SoundCloudTrackSummary?` field
- [x] 2.2 Add quota check in `SoundCloudPublisher.publish()` before upload — call `getMe()`, check `uploadSecondsLeft`, throw exception with oldest track filtered by podcast name

## 3. Publishing Error Handling

- [x] 3.1 Add catch for `SoundCloudQuotaExceededException` in `PublishingController.publish()` returning HTTP 413 with `code: "quota_exceeded"` and oldest track info
- [x] 3.2 Add catch for `HttpClientErrorException.Unauthorized` returning HTTP 401 with `code: "oauth_expired"`
- [x] 3.3 Detect re-authorize/refresh-failed messages in `IllegalStateException` handler and return HTTP 401

## 4. SoundCloud Account Endpoints

- [x] 4.1 Enrich `GET /users/{userId}/oauth/soundcloud/status` to include `quota` object from SoundCloud `/me` API
- [x] 4.2 Add `DELETE /users/{userId}/oauth/soundcloud/tracks/{trackId}` endpoint to delete a track from SoundCloud

## 5. Frontend Publish Wizard

- [x] 5.1 Add `isOAuthExpired` state and handle HTTP 401 in publish response
- [x] 5.2 Show "Re-authorize SoundCloud" button with KeyRound icon on OAuth expiry
- [x] 5.3 Add `oldestTrack` state and handle HTTP 413 in publish response
- [x] 5.4 Show oldest track title and destructive "Remove Track" button on quota exceeded
- [x] 5.5 On successful track deletion, return to confirm step for retry

## 6. Tests

- [x] 6.1 Add default `getMe` mock to `SoundCloudPublisherTest` for existing publish tests
- [x] 6.2 Add test for quota exceeded exception in `SoundCloudPublisherTest`
- [x] 6.3 Add test for unlimited quota bypass in `SoundCloudPublisherTest`

# Delta: SoundCloud Integration

## NEW Requirements

### Requirement: SoundCloud upload quota check
The system SHALL check the user's SoundCloud upload quota before attempting a track upload by calling `GET https://api.soundcloud.com/me` with the user's access token and reading the `quota` object. If `quota.unlimitedUploadQuota` is `false` and `quota.uploadSecondsLeft <= 0`, the system SHALL throw a `SoundCloudQuotaExceededException` containing a descriptive error message and the oldest deletable track (if available). The oldest track is determined by calling `GET https://api.soundcloud.com/me/tracks`, filtering to tracks whose title starts with the podcast name, and selecting the one with the earliest `createdAt`.

#### Scenario: Quota available
- **WHEN** the user has upload seconds remaining (`uploadSecondsLeft > 0`)
- **THEN** the publish flow proceeds normally

#### Scenario: Quota exceeded
- **WHEN** the user has no upload seconds remaining (`uploadSecondsLeft <= 0`) and `unlimitedUploadQuota` is `false`
- **THEN** the system returns HTTP 413 with `code: "quota_exceeded"`, the error message, and `oldestTrack` info (id, title, createdAt, duration)

#### Scenario: Unlimited quota
- **WHEN** the user has `unlimitedUploadQuota: true`
- **THEN** the publish flow proceeds normally regardless of `uploadSecondsLeft`

#### Scenario: Oldest track filtering
- **WHEN** the SoundCloud account has tracks from multiple sources
- **THEN** only tracks whose title starts with the podcast name are considered for the oldest track suggestion

### Requirement: SoundCloud track deletion
The system SHALL provide a `DELETE /users/{userId}/oauth/soundcloud/tracks/{trackId}` endpoint that deletes a track from SoundCloud using the user's stored access token. If SoundCloud returns 404 (track already deleted), the system SHALL treat it as a successful deletion.

#### Scenario: Successful deletion
- **WHEN** a DELETE request is made for a valid track ID
- **THEN** the system calls `DELETE https://api.soundcloud.com/tracks/{trackId}` with the user's access token and returns HTTP 200 with `{"deleted": true, "trackId": <id>}`

#### Scenario: Track already deleted
- **WHEN** SoundCloud returns 404 for the track deletion
- **THEN** the system treats it as successful and returns HTTP 200

#### Scenario: No SoundCloud connection
- **WHEN** the user has no stored SoundCloud OAuth connection
- **THEN** the system returns HTTP 400

### Requirement: OAuth expiry detection in publishing
The system SHALL detect OAuth-related failures during publishing and return HTTP 401 with `code: "oauth_expired"`. This covers both `HttpClientErrorException.Unauthorized` thrown directly by the SoundCloud API and `IllegalStateException` messages containing "re-authorize" or "refresh failed" from the token manager.

#### Scenario: Direct 401 from SoundCloud API
- **WHEN** the SoundCloud API returns 401 during publishing
- **THEN** the system returns HTTP 401 with `{"error": "SoundCloud authorization expired. Please re-authorize your account.", "code": "oauth_expired"}`

#### Scenario: Token refresh failure
- **WHEN** the token manager fails to refresh the access token and throws an `IllegalStateException` with a re-authorize message
- **THEN** the system returns HTTP 401 with `code: "oauth_expired"`

### Requirement: Frontend re-authorize and quota recovery
The publish wizard SHALL display contextual recovery actions based on the error type. On HTTP 401 with `code: "oauth_expired"`, a "Re-authorize SoundCloud" button SHALL be shown that fetches the authorization URL and opens it in a new tab. On HTTP 413 with `code: "quota_exceeded"`, the oldest track title SHALL be displayed with a "Remove Track" button that calls the delete endpoint and returns to the confirm step on success.

#### Scenario: OAuth expired error
- **WHEN** publishing fails with HTTP 401
- **THEN** the wizard shows the error message and a "Re-authorize SoundCloud" button with a KeyRound icon

#### Scenario: Quota exceeded with oldest track
- **WHEN** publishing fails with HTTP 413 and an `oldestTrack` is returned
- **THEN** the wizard shows the error message, the oldest track's title, and a destructive "Remove Track" button

#### Scenario: Track removed successfully
- **WHEN** the user clicks "Remove Track" and the deletion succeeds
- **THEN** the wizard returns to the confirm step so the user can retry publishing

## MODIFIED Requirements

### Requirement: SoundCloud connection status (MODIFIED)
The `GET /users/{userId}/oauth/soundcloud/status` endpoint SHALL additionally return a `quota` object when the user is connected. The quota is fetched from SoundCloud's `/me` API and includes `unlimitedUploadQuota` (boolean), `uploadSecondsUsed` (long), and `uploadSecondsLeft` (long). If the quota fetch fails, `quota` SHALL be `null`.

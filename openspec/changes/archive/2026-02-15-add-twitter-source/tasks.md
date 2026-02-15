## 1. X Configuration

- [x] 1.1 Add `XProperties` (clientId, clientSecret) to `AppProperties` with `app.x.*` prefix
- [x] 1.2 Document `APP_X_CLIENT_ID` and `APP_X_CLIENT_SECRET` environment variables

## 2. X OAuth Flow

- [x] 2.1 Create `XClient` component with `RestClient` for X API v2 calls (token exchange, token refresh, username resolution, timeline fetch)
- [x] 2.2 Create `XOAuthController` with authorization endpoint (`GET /users/{userId}/oauth/x/authorize`) using PKCE and HMAC-signed state
- [x] 2.3 Add callback endpoint (`GET /oauth/x/callback`) that exchanges code for tokens and stores in `oauth_connections` with provider `"x"`
- [x] 2.4 Add status endpoint (`GET /users/{userId}/oauth/x/status`) and disconnect endpoint (`DELETE /users/{userId}/oauth/x`)
- [x] 2.5 Create `XTokenManager` with proactive token refresh (5-minute buffer before 2-hour expiry)

## 3. TwitterFetcher Implementation

- [x] 3.1 Create `TwitterFetcher` component that uses `XTokenManager` and `XClient`
- [x] 3.2 Implement username extraction from URL or plain username input
- [x] 3.3 Implement username-to-user-ID resolution via `GET /2/users/by/username/:username`
- [x] 3.4 Implement user timeline fetching via `GET /2/users/:id/tweets` with `since_id` support
- [x] 3.5 Implement tweet-to-article mapping (title truncation, URL construction, author formatting)
- [x] 3.6 Implement `lastSeenId` parsing and caching (`<userId>:<sinceId>` format)
- [x] 3.7 Implement error handling for 429 (rate limit), 401/403 (auth), 404 (user not found), and 5xx responses

## 4. Source Poller Integration

- [x] 4.1 Add `"twitter"` branch to the `when` expression in `SourcePoller.poll()`
- [x] 4.2 Update `SourcePoller` to accept and pass user ID to `TwitterFetcher` for OAuth token resolution
- [x] 4.3 Update `SourcePollingScheduler` to resolve podcast owner user ID and pass it through the polling chain

## 5. Testing

- [x] 5.1 Write unit tests for `XOAuthController` (authorization URL generation, callback handling) using MockK
- [x] 5.2 Write unit tests for `XTokenManager` (valid token, refresh, expired refresh) using MockK
- [x] 5.3 Write unit tests for `TwitterFetcher` (username extraction, tweet mapping, lastSeenId parsing) using MockK
- [x] 5.4 Write unit tests for `TwitterFetcher` error handling (429, 401, 404, 5xx, missing OAuth connection) using MockK
- [x] 5.5 Write unit tests for `SourcePoller` with Twitter source type dispatch
- [x] 5.6 Verify existing `SourcePoller` tests still pass with the updated method signature

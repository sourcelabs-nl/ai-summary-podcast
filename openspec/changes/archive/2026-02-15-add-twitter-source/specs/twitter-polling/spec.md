## ADDED Requirements

### Requirement: X (Twitter) user timeline polling
The system SHALL fetch recent posts from X user timelines using the X API v2 endpoint `GET https://api.x.com/2/users/:id/tweets` with OAuth 2.0 user context. A `TwitterFetcher` component SHALL accept a source URL (either a plain username like `elonmusk` or a full URL like `https://x.com/elonmusk`), extract the username, resolve it to a user ID, and fetch recent tweets. The fetcher SHALL use the `since_id` parameter for incremental polling to avoid re-fetching already-seen tweets. The fetcher SHALL request `tweet.fields=text,created_at,author_id` to get full tweet data.

#### Scenario: Fetch tweets from a user timeline by username
- **WHEN** a source with `type: "twitter"` and `url: "elonmusk"` is polled
- **THEN** the system resolves the username to a user ID via `GET https://api.x.com/2/users/by/username/elonmusk`, fetches recent tweets, and returns them as articles

#### Scenario: Fetch tweets from a full X URL
- **WHEN** a source with `type: "twitter"` and `url: "https://x.com/elonmusk"` is polled
- **THEN** the system extracts `elonmusk` from the URL, resolves to a user ID, and fetches tweets as articles

#### Scenario: Incremental polling with since_id
- **WHEN** a Twitter source is polled and `lastSeenId` contains a previously stored value in `<userId>:<sinceId>` format
- **THEN** the system uses the cached user ID (skipping username resolution) and passes `since_id` to the timeline endpoint to only fetch newer tweets

#### Scenario: First poll of a new Twitter source
- **WHEN** a Twitter source is polled for the first time (`lastSeenId` is null)
- **THEN** the system resolves the username to a user ID, fetches the most recent tweets (up to API default), and stores `<userId>:<latestTweetId>` as the new `lastSeenId`

### Requirement: Tweet-to-article mapping
The system SHALL map each tweet to an article with the following field mapping:
- `title`: First 100 characters of tweet text, appended with "..." if truncated
- `body`: Full tweet text
- `url`: `https://x.com/<username>/status/<tweetId>`
- `publishedAt`: Tweet's `created_at` field (ISO-8601 format)
- `author`: `@<username>`
- `contentHash`: Empty string (computed by `SourcePoller`)

#### Scenario: Tweet mapped to article with short text
- **WHEN** a tweet has text "Breaking: major policy change announced today" (under 100 chars)
- **THEN** the article title is "Breaking: major policy change announced today" (no truncation) and the body is the same text

#### Scenario: Tweet mapped to article with long text
- **WHEN** a tweet has text longer than 100 characters
- **THEN** the article title is the first 100 characters followed by "..." and the body contains the full text

#### Scenario: Tweet URL constructed correctly
- **WHEN** a tweet with ID `1234567890` is from user `techreporter`
- **THEN** the article URL is `https://x.com/techreporter/status/1234567890`

### Requirement: X API authentication via OAuth connection
The `TwitterFetcher` SHALL retrieve a valid X OAuth access token via `XTokenManager.getValidAccessToken(userId)`, which automatically handles token refresh. The fetcher SHALL include the token as `Authorization: Bearer <token>` in all X API requests. If no X OAuth connection exists for the user, the fetcher SHALL throw an error that is caught by `SourcePoller` and logged.

#### Scenario: Access token resolved from OAuth connection
- **WHEN** a Twitter source is polled and the user has an X OAuth connection
- **THEN** the `XTokenManager` returns a valid access token (refreshing if needed) and it is used in the `Authorization` header

#### Scenario: No X OAuth connection for user
- **WHEN** a Twitter source is polled but the user has no X OAuth connection
- **THEN** an error is thrown and logged, and the source is skipped for this polling cycle

#### Scenario: X integration not configured on server
- **WHEN** a Twitter source is polled but `APP_X_CLIENT_ID` is not configured
- **THEN** a warning is logged and the source is skipped for this polling cycle

### Requirement: X API error handling
The `TwitterFetcher` SHALL handle X API errors gracefully. Rate limit responses (HTTP 429) SHALL be logged as warnings and the source SHALL be skipped until the next poll cycle. Authentication errors (HTTP 401/403) SHALL be logged as errors. Other API errors SHALL be logged and the source skipped.

#### Scenario: Rate limit exceeded
- **WHEN** the X API returns HTTP 429 during polling
- **THEN** a warning is logged with the rate limit reset time (if available) and no articles are returned

#### Scenario: Authentication failure
- **WHEN** the X API returns HTTP 401 or 403
- **THEN** an error is logged indicating the OAuth token may be invalid or revoked, and no articles are returned

#### Scenario: X user not found
- **WHEN** the username resolution endpoint returns HTTP 404 or an empty result
- **THEN** an error is logged indicating the X user was not found, and no articles are returned

#### Scenario: Network or server error
- **WHEN** the X API returns HTTP 5xx or the request times out
- **THEN** an error is logged and no articles are returned

### Requirement: Username-to-user-ID caching in lastSeenId
The system SHALL cache the resolved X user ID in the source's `lastSeenId` field using the format `<userId>:<sinceId>`. On subsequent polls, the system SHALL parse the cached user ID from `lastSeenId` instead of making an additional API call to resolve the username. If the source URL changes (different username), the system SHALL detect the mismatch and re-resolve.

#### Scenario: User ID cached after first resolution
- **WHEN** username `techreporter` resolves to user ID `9876543210` and the latest tweet ID is `1111111111`
- **THEN** the source's `lastSeenId` is stored as `9876543210:1111111111`

#### Scenario: Cached user ID reused on subsequent poll
- **WHEN** a Twitter source has `lastSeenId` = `9876543210:1111111111`
- **THEN** the system uses user ID `9876543210` directly and passes `since_id=1111111111` to the API

#### Scenario: No new tweets returns empty list
- **WHEN** the timeline endpoint returns no tweets newer than `since_id`
- **THEN** the fetcher returns an empty list and `lastSeenId` remains unchanged

## Context

The pipeline currently supports two source types: RSS feeds (`RssFeedFetcher`) and websites (`WebsiteFetcher`). Source type dispatch happens via a `when` expression on the string `source.type` in `SourcePoller.poll()`. Adding a new source type requires: a new fetcher component, a new branch in the `when` expression, and a way to authenticate with the external API.

X (Twitter) API v2 provides a user timeline endpoint that returns recent tweets for a given user ID. Authentication can use OAuth 2.0 with PKCE, which allows individual users to authorize with their regular X account — no developer account needed per user.

The project already implements OAuth 2.0 PKCE for SoundCloud with the following components:
- `SoundCloudOAuthController` — authorization initiation and callback
- `SoundCloudTokenManager` — proactive token refresh (5-min buffer)
- `SoundCloudClient` — API calls with Bearer token
- `OAuthConnectionService` — encrypted token storage/retrieval
- `oauth_connections` table — generic, supports multiple providers

## Goals / Non-Goals

**Goals:**
- Fetch recent posts from X user timelines and store them as articles
- Authenticate via OAuth 2.0 PKCE so individual users only need to authorize with their X account
- Reuse the existing `oauth_connections` table and `OAuthConnectionService`
- Follow the SoundCloud OAuth pattern for consistency
- Handle X API rate limits and errors gracefully

**Non-Goals:**
- X search/query-based polling (only user timelines for now)
- Fetching replies, retweets, or quoted tweets (only original posts)
- Media/image extraction from tweets
- Real-time streaming (polling only, consistent with existing architecture)

## Decisions

### Decision 1: OAuth 2.0 PKCE flow (like SoundCloud)

**Choice:** Implement OAuth 2.0 with PKCE for X authentication. The app operator configures X developer credentials (`APP_X_CLIENT_ID`, `APP_X_CLIENT_SECRET`), and users authorize via the standard OAuth flow.

**Rationale:** This matches the SoundCloud pattern exactly. Users don't need their own developer account — they just click "Authorize" and log in with their X account. The `oauth_connections` table already supports multiple providers.

**Alternatives considered:**
- App-only Bearer token (per-user developer accounts): Requires each user to pay $100/month for X Basic tier — too expensive and complex
- Web scraping: Fragile, against ToS

### Decision 2: Use X API v2 user timeline endpoint

**Choice:** Call `GET https://api.x.com/2/users/:id/tweets` with OAuth 2.0 user context to fetch recent tweets.

**Rationale:** This is the standard v2 endpoint for getting a user's timeline. With user-context OAuth, the app uses the authorized user's read permissions. Supports `since_id` for incremental polling.

### Decision 3: Mirror SoundCloud component structure

**Choice:** Create `XOAuthController`, `XTokenManager`, and `XClient` following the same patterns as the SoundCloud equivalents.

**Rationale:** Consistency. Developers familiar with the SoundCloud OAuth code can immediately understand the X integration. The same `OAuthConnectionService` and `ApiKeyEncryptor` are reused.

Component mapping:
- `XOAuthController` → authorization (`/users/{userId}/oauth/x/authorize`) and callback (`/oauth/x/callback`)
- `XTokenManager` → proactive token refresh (X tokens expire after 2 hours)
- `XClient` → REST calls to X API v2 (username resolution, timeline fetch, token exchange/refresh)

### Decision 4: Username-to-user-ID resolution

**Choice:** The `TwitterFetcher` resolves X usernames to user IDs via `GET https://api.x.com/2/users/by/username/:username` and caches the result in the source's `lastSeenId` field as `<userId>:<lastTweetId>` format.

**Rationale:** The timeline endpoint requires a numeric user ID, but users configure sources with usernames (more intuitive). We encode both the resolved user ID and the pagination cursor in `lastSeenId` to avoid an extra API call on every poll. Format: `<userId>:<sinceId>` (e.g., `12345:1234567890`).

### Decision 5: Tweet-to-article mapping

**Choice:**
- `article.title` = first 100 characters of tweet text + "..." if truncated
- `article.body` = full tweet text
- `article.url` = `https://x.com/<username>/status/<tweetId>`
- `article.publishedAt` = tweet `created_at` (ISO-8601)
- `article.author` = `@<username>`

**Rationale:** Tweets don't have titles, so we synthesize one from the text. The body contains the full tweet for LLM processing. The URL points to the tweet on X for reference.

### Decision 6: Source URL format

**Choice:** Accept either a plain username (e.g., `elonmusk`) or a full X URL (e.g., `https://x.com/elonmusk`). The fetcher extracts the username from either format.

**Rationale:** Flexibility for users. Both formats are intuitive.

### Decision 7: X OAuth scopes

**Choice:** Request `tweet.read users.read offline.access` scopes.

**Rationale:** `tweet.read` for timeline access, `users.read` for username-to-ID resolution, `offline.access` for refresh tokens (X tokens expire after 2 hours).

### Decision 8: X configuration via environment variables

**Choice:** Add `XProperties` to `AppProperties` with `clientId` and `clientSecret`, configured via `APP_X_CLIENT_ID` and `APP_X_CLIENT_SECRET`.

**Rationale:** Follows the SoundCloud pattern. The app operator (not individual users) provides the developer credentials. OAuth endpoints return 503 if not configured.

## Risks / Trade-offs

- **[X API cost for app operator]** → The app operator needs an X developer account (Basic tier, $100/month) for the client credentials. Individual users authorize for free. This is the same model as SoundCloud.
- **[X API rate limits]** → The Basic tier allows 10,000 tweets read/month. The fetcher logs 429 responses and skips the source until the next poll cycle. The default poll interval (60 min) is conservative.
- **[Token expiry]** → X OAuth tokens expire after 2 hours (much shorter than SoundCloud's non-expiring tokens). The `XTokenManager` proactively refreshes before expiry, matching the SoundCloud pattern with a 5-minute buffer.
- **[Username changes]** → If an X user changes their username, the cached user ID in `lastSeenId` still works. If the source URL is updated, the fetcher re-resolves the user ID.
- **[OAuth connection required]** → Users must complete the OAuth flow before adding Twitter sources. If the connection is missing or expired, polling logs a warning and skips the source.

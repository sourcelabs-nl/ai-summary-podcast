## Why

The pipeline currently supports RSS feeds and websites as content sources, but X (Twitter) is one of the most important real-time information sources. Many experts, journalists, and organizations share insights and breaking news exclusively on X. Adding X as a source type allows users to monitor specific accounts and include their posts in podcast briefings.

## What Changes

- Add a new `TwitterFetcher` component that fetches recent posts from X accounts using the X API v2
- Extend `SourcePoller` to dispatch `"twitter"` source types to the new fetcher
- Users configure X sources with `type: "twitter"` and `url` set to the X username (e.g., `elonmusk` or `https://x.com/elonmusk`)
- Add OAuth 2.0 PKCE flow for X authentication, following the existing SoundCloud OAuth pattern: app operator provides X developer credentials, individual users authorize with their X account
- Store X OAuth tokens in the existing `oauth_connections` table (provider: `"x"`)
- Add `XOAuthController`, `XTokenManager`, and `XClient` components mirroring the SoundCloud pattern
- Posts are extracted as articles with tweet text as body, author handle as author, and tweet URL as url

## Capabilities

### New Capabilities
- `twitter-polling`: Fetching and extracting posts from X accounts via the X API v2, converting them to articles
- `twitter-oauth`: OAuth 2.0 PKCE authorization flow for X, token storage, and automatic refresh

### Modified Capabilities
- `source-polling`: Add `"twitter"` as a recognized source type in the polling dispatch logic
- `podcast-sources`: Add `"twitter"` as a valid source type

## Impact

- **New dependency**: HTTP client calls to X API v2 (`api.x.com/2/`) — uses Spring's `RestClient`
- **OAuth flow**: New endpoints for X authorization (`/oauth/x/authorize`, `/oauth/x/callback`), following the SoundCloud pattern
- **Configuration**: App operator must set `APP_X_CLIENT_ID` and `APP_X_CLIENT_SECRET` environment variables (requires X developer Basic tier)
- **Source controller**: No structural changes — `type: "twitter"` works with existing CRUD
- **Database**: No schema changes — reuses existing `oauth_connections` table with provider `"x"`
- **Rate limits**: X API v2 has rate limits; the fetcher should handle 429 responses gracefully

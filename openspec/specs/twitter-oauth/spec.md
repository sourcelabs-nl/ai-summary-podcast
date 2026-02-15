# Capability: Twitter OAuth

## Purpose

OAuth 2.0 PKCE authorization flow for X (Twitter), token storage, and automatic refresh.

## Requirements

### Requirement: X OAuth 2.0 PKCE authorization flow
The system SHALL provide an OAuth 2.0 PKCE authorization flow for X, following the same pattern as SoundCloud. An `XOAuthController` SHALL expose an authorization endpoint at `GET /users/{userId}/oauth/x/authorize` that generates a PKCE code verifier and challenge, creates an HMAC-signed state parameter, stores the code verifier in memory, and returns the X authorization URL. The authorization URL SHALL point to `https://twitter.com/i/oauth2/authorize` with `response_type=code`, `code_challenge_method=S256`, and scopes `tweet.read users.read offline.access`. The callback endpoint at `GET /oauth/x/callback` SHALL verify the state signature, exchange the authorization code for tokens via `POST https://api.x.com/2/oauth2/token`, encrypt the tokens, and store them in the `oauth_connections` table with provider `"x"`.

#### Scenario: Initiate X authorization
- **WHEN** a `GET /users/{userId}/oauth/x/authorize` request is received for an existing user
- **THEN** the system returns the X authorization URL with PKCE challenge and signed state parameter

#### Scenario: Initiate authorization when X is not configured
- **WHEN** a `GET /users/{userId}/oauth/x/authorize` request is received but `APP_X_CLIENT_ID` or `APP_X_CLIENT_SECRET` is not set
- **THEN** the system returns HTTP 503 indicating X integration is not configured

#### Scenario: OAuth callback with valid code
- **WHEN** the X callback is received with a valid authorization code and state
- **THEN** the system exchanges the code for tokens, encrypts and stores them in `oauth_connections` with provider `"x"`, and returns an HTML success page

#### Scenario: OAuth callback with invalid state
- **WHEN** the X callback is received with an invalid or tampered state parameter
- **THEN** the system returns HTTP 400

#### Scenario: OAuth callback with expired code verifier
- **WHEN** the X callback is received but the code verifier has expired (older than 10 minutes)
- **THEN** the system returns HTTP 404

### Requirement: X OAuth token refresh
The system SHALL proactively refresh X OAuth tokens before they expire. An `XTokenManager` SHALL check if the stored access token expires within 5 minutes and, if so, refresh it using the stored refresh token via `POST https://api.x.com/2/oauth2/token` with `grant_type=refresh_token`. The refreshed tokens SHALL be re-encrypted and stored. X access tokens expire after 2 hours.

#### Scenario: Token still valid
- **WHEN** `getValidAccessToken()` is called and the token expires in more than 5 minutes
- **THEN** the stored access token is returned without refreshing

#### Scenario: Token about to expire
- **WHEN** `getValidAccessToken()` is called and the token expires within 5 minutes
- **THEN** the system refreshes the token, stores the new encrypted tokens, and returns the new access token

#### Scenario: Refresh token fails
- **WHEN** the token refresh request to X fails (e.g., refresh token revoked)
- **THEN** an error is thrown indicating the user must re-authorize their X connection

### Requirement: X OAuth connection status and disconnect
The system SHALL expose endpoints to check X connection status and disconnect. `GET /users/{userId}/oauth/x/status` SHALL return whether the user has an X OAuth connection, its scopes, and connection timestamp. `DELETE /users/{userId}/oauth/x` SHALL remove the connection.

#### Scenario: Check status when connected
- **WHEN** a `GET /users/{userId}/oauth/x/status` request is received for a user with an X OAuth connection
- **THEN** the system returns `{ "connected": true, "scopes": "tweet.read users.read offline.access", "connectedAt": "..." }`

#### Scenario: Check status when not connected
- **WHEN** a `GET /users/{userId}/oauth/x/status` request is received for a user without an X OAuth connection
- **THEN** the system returns `{ "connected": false }`

#### Scenario: Disconnect X account
- **WHEN** a `DELETE /users/{userId}/oauth/x` request is received for a user with an X OAuth connection
- **THEN** the connection is deleted and HTTP 204 is returned

#### Scenario: Disconnect when not connected
- **WHEN** a `DELETE /users/{userId}/oauth/x` request is received for a user without an X OAuth connection
- **THEN** HTTP 404 is returned

### Requirement: X configuration via environment variables
The system SHALL require `APP_X_CLIENT_ID` and `APP_X_CLIENT_SECRET` environment variables for X integration. These SHALL be configured via `AppProperties` as `app.x.client-id` and `app.x.client-secret`. All X OAuth endpoints SHALL return HTTP 503 if these are not configured. The redirect URI SHALL be derived from `app.feed.base-url` + `/oauth/x/callback`.

#### Scenario: X credentials configured
- **WHEN** the application starts with both `APP_X_CLIENT_ID` and `APP_X_CLIENT_SECRET` set
- **THEN** X OAuth endpoints are functional

#### Scenario: X credentials not configured
- **WHEN** the application starts without X credentials
- **THEN** X OAuth endpoints return HTTP 503, and polling of Twitter sources logs a warning and skips

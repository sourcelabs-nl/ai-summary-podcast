# Capability: Publishing Credentials

## Purpose

Storage, testing, and resolution of publication provider credentials for FTP and SoundCloud publishing.

## Requirements

### Requirement: Publication credential storage
The system SHALL store publication provider credentials in the `user_provider_configs` table under category `PUBLISHING`. The `encrypted_api_key` column SHALL contain an encrypted JSON string with provider-specific fields:
- Provider `ftp`: `{"host": "...", "port": 21, "username": "...", "password": "...", "useTls": true}`
- Provider `soundcloud`: `{"clientId": "...", "clientSecret": "...", "callbackUri": "..."}`

The `base_url` column SHALL be null for publishing providers (not meaningful).

#### Scenario: Store FTP credentials
- **WHEN** a `PUT /users/{userId}/api-keys/PUBLISHING` request is received with `{"provider": "ftp", "apiKey": "{\"host\":\"ftp.example.com\",\"port\":21,\"username\":\"user\",\"password\":\"pass\",\"useTls\":true}"}`
- **THEN** the JSON is encrypted and stored in `user_provider_configs` with category `PUBLISHING` and provider `ftp`

#### Scenario: Store SoundCloud credentials
- **WHEN** a `PUT /users/{userId}/api-keys/PUBLISHING` request is received with `{"provider": "soundcloud", "apiKey": "{\"clientId\":\"abc\",\"clientSecret\":\"xyz\",\"callbackUri\":\"https://example.com/callback\"}"}`
- **THEN** the JSON is encrypted and stored with category `PUBLISHING` and provider `soundcloud`

#### Scenario: List publishing credentials
- **WHEN** a `GET /users/{userId}/api-keys` request is received for a user with FTP and SoundCloud publishing credentials
- **THEN** both are included in the response with category `PUBLISHING` -- without exposing the encrypted values

### Requirement: Test connection endpoint
The system SHALL provide a `POST /users/{userId}/publishing/test/{target}` endpoint that tests connectivity for a publication target without persisting credentials. The response SHALL include `success` (boolean) and `message` (String).

For target `ftp`: the request body SHALL contain `{"host": "...", "port": 21, "username": "...", "password": "...", "useTls": true}`. The endpoint SHALL connect to the FTP(S) server, authenticate, and list the root directory. On success, return `{"success": true, "message": "Connected successfully"}`. On failure, return `{"success": false, "message": "<error detail>"}`.

For target `soundcloud`: no request body is needed. The endpoint SHALL verify the user's existing OAuth connection by calling the SoundCloud `/me` API and return quota information. On success, return `{"success": true, "message": "Connected as <username>", "quota": {"uploadSecondsUsed": ..., "uploadSecondsLeft": ...}}`. On failure, return `{"success": false, "message": "<error detail>"}`.

#### Scenario: Test FTP connection success
- **WHEN** a `POST /users/{userId}/publishing/test/ftp` request is received with valid FTP credentials
- **THEN** the system connects, authenticates, and returns `{"success": true, "message": "Connected successfully"}`

#### Scenario: Test FTP connection failure -- wrong host
- **WHEN** a `POST /users/{userId}/publishing/test/ftp` request is received with an unreachable host
- **THEN** the system returns `{"success": false, "message": "Connection refused: ..."}`

#### Scenario: Test FTP connection failure -- wrong credentials
- **WHEN** a `POST /users/{userId}/publishing/test/ftp` request is received with wrong username/password
- **THEN** the system returns `{"success": false, "message": "Authentication failed"}`

#### Scenario: Test FTPS connection
- **WHEN** a `POST /users/{userId}/publishing/test/ftp` request is received with `useTls: true`
- **THEN** the system tests using FTPS (explicit TLS)

#### Scenario: Test SoundCloud connection success
- **WHEN** a `POST /users/{userId}/publishing/test/soundcloud` request is received for a user with a valid OAuth connection
- **THEN** the system returns `{"success": true, "message": "Connected as <username>", "quota": {...}}`

#### Scenario: Test SoundCloud connection -- no OAuth connection
- **WHEN** a `POST /users/{userId}/publishing/test/soundcloud` request is received for a user without a SoundCloud OAuth connection
- **THEN** the system returns `{"success": false, "message": "No SoundCloud connection. Please authorize first."}`

#### Scenario: Test SoundCloud connection -- expired token
- **WHEN** a `POST /users/{userId}/publishing/test/soundcloud` request is received and the OAuth token is expired and refresh fails
- **THEN** the system returns `{"success": false, "message": "SoundCloud authorization expired. Please re-authorize."}`

#### Scenario: Unsupported target
- **WHEN** a `POST /users/{userId}/publishing/test/youtube` request is received
- **THEN** the system returns HTTP 400 with an error indicating the target is not supported

### Requirement: SoundCloud credential resolution from user_provider_configs
The SoundCloud OAuth flow and publisher SHALL resolve `clientId`, `clientSecret`, and `callbackUri` by:
1. Looking up `user_provider_configs` where `userId` matches, `category = PUBLISHING`, and `provider = soundcloud`
2. If found, decrypt and parse the JSON to extract `clientId`, `clientSecret`, and `callbackUri`
3. If not found, fall back to `AppProperties` (`app.soundcloud.client-id`, `app.soundcloud.client-secret`) and derive `callbackUri` from `app.feed.base-url` + `/oauth/soundcloud/callback`
4. If neither source provides credentials, return an error indicating SoundCloud is not configured

#### Scenario: Credentials from database
- **WHEN** the SoundCloud OAuth flow is initiated and the user has a `PUBLISHING/soundcloud` config in `user_provider_configs`
- **THEN** the system uses the DB-stored `clientId`, `clientSecret`, and `callbackUri`

#### Scenario: Credentials from env var fallback
- **WHEN** the SoundCloud OAuth flow is initiated and the user has no DB config but `APP_SOUNDCLOUD_CLIENT_ID` and `APP_SOUNDCLOUD_CLIENT_SECRET` are set
- **THEN** the system uses the env var values and derives the callback URI from `app.feed.base-url`

#### Scenario: No credentials available
- **WHEN** the SoundCloud OAuth flow is initiated and neither DB config nor env vars are available
- **THEN** the system returns HTTP 400 indicating SoundCloud credentials must be configured

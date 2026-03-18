# Capability: FTP Publishing

## Purpose

Publishes podcast episodes, feed XML, sources files, and artwork to an FTP(S) server.

## Requirements

### Requirement: FTP publisher implementation
The system SHALL provide an `FtpPublisher` Spring component implementing the `EpisodePublisher` interface with `targetName()` returning `"ftp"`. The `publish` method SHALL:
1. Resolve FTP credentials from `user_provider_configs` (category `PUBLISHING`, provider `ftp`)
2. Read `remotePath` and `publicUrl` from the podcast's `podcast_publication_targets` config for target `"ftp"`
3. Connect to the FTP server using the credentials (host, port, username, password)
4. If `useTls` is `true`, use FTPS (explicit TLS via `FTPSClient`); otherwise use plain `FTPClient`
5. Upload the episode's `sources.md` file to `{remotePath}/{slug}-sources.md`
6. Upload the episode's MP3 file to `{remotePath}/{filename}` in binary transfer mode
7. Regenerate and upload `feed.xml` to `{remotePath}/feed.xml` (using `publicUrl` as base URL)
8. Upload podcast image (if exists) to `{remotePath}/podcast-image.{ext}`
9. Return a `PublishResult` with `externalId` set to the remote file path and `externalUrl` set to `{publicUrl}/{filename}`

The filename SHALL be derived from the podcast name and episode date: `{podcast-name}-{date}.mp3` (lowercased, non-alphanumeric replaced with hyphens).

#### Scenario: Successful FTP upload with all files
- **WHEN** the FTP publisher is called for a podcast with configured FTP credentials, an enabled FTP target with `remotePath = "/shows/tech/"` and `publicUrl = "https://podcast.example.com/shows/tech/"`, and a podcast image
- **THEN** the following files are uploaded to `/shows/tech/`: `tech-news-2026-03-12-sources.md`, `tech-news-2026-03-12.mp3`, `feed.xml`, `podcast-image.jpg`, and a `PublishResult` is returned with `externalUrl = "https://podcast.example.com/shows/tech/tech-news-2026-03-12.mp3"`

#### Scenario: Successful FTPS upload
- **WHEN** the FTP publisher is called and the user's FTP credentials have `useTls = true`
- **THEN** the connection uses explicit TLS (FTPS) and all uploads succeed

#### Scenario: FTP credentials not configured
- **WHEN** the FTP publisher is called for a user with no FTP credentials in `user_provider_configs`
- **THEN** an exception is thrown indicating FTP credentials must be configured first

#### Scenario: FTP target not configured on podcast
- **WHEN** the FTP publisher is called but the podcast has no `podcast_publication_targets` entry for `"ftp"`
- **THEN** an exception is thrown indicating the FTP target must be configured on the podcast

#### Scenario: Connection failure
- **WHEN** the FTP server is unreachable or rejects the credentials
- **THEN** an exception is thrown with a descriptive error message (e.g., "Connection refused" or "Authentication failed")

#### Scenario: Upload failure
- **WHEN** the FTP connection succeeds but the file upload fails (e.g., permission denied, disk full)
- **THEN** an exception is thrown with the FTP server's reply message

#### Scenario: No podcast image
- **WHEN** the FTP publisher is called for a podcast without a stored image
- **THEN** the MP3, sources.md, and feed.xml are uploaded but no image upload is attempted

### Requirement: FTP feed.xml generation uses publicUrl
When the FTP publisher regenerates the `feed.xml` for upload, it SHALL pass the FTP target's `publicUrl` as the base URL to the feed generator. This ensures enclosure URLs, image URLs, and sources.md links in the uploaded feed point to the public-facing URL where files are hosted.

#### Scenario: Feed.xml uses publicUrl for enclosures
- **WHEN** the FTP publisher generates feed.xml with `publicUrl = "https://podcast.example.com/shows/tech/"`
- **THEN** enclosure URLs are `https://podcast.example.com/shows/tech/{filename}` and the image URL is `https://podcast.example.com/shows/tech/podcast-image.jpg`

### Requirement: FTP client resource cleanup
The `FtpPublisher` SHALL always disconnect from the FTP server after the upload attempt, whether successful or failed. The disconnect SHALL happen in a `finally` block to prevent connection leaks.

#### Scenario: Cleanup after successful upload
- **WHEN** an FTP upload completes successfully
- **THEN** the FTP connection is closed

#### Scenario: Cleanup after failed upload
- **WHEN** an FTP upload fails with an exception
- **THEN** the FTP connection is still closed before the exception propagates

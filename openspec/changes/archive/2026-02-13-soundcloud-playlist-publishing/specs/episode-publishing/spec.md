## ADDED Requirements

### Requirement: SoundCloud playlist ID storage on podcast
The `podcasts` table SHALL have a nullable `soundcloud_playlist_id` column (TEXT) that stores the SoundCloud playlist ID associated with the podcast. The `Podcast` entity SHALL include a `soundcloudPlaylistId` field. A database migration SHALL add this column.

#### Scenario: New podcast has no playlist ID
- **WHEN** a new podcast is created
- **THEN** the `soundcloud_playlist_id` column is null

#### Scenario: Playlist ID stored after first SoundCloud publish
- **WHEN** the first episode of a podcast is published to SoundCloud and a playlist is created
- **THEN** the `soundcloud_playlist_id` column is updated with the SoundCloud playlist ID

#### Scenario: Playlist ID updated when playlist is recreated
- **WHEN** a publish detects a stale playlist (404) and creates a new one
- **THEN** the `soundcloud_playlist_id` column is updated with the new playlist ID

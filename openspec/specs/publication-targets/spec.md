# Capability: Publication Targets

## Purpose

Per-podcast publication target configuration with CRUD API, database storage, and migration from legacy SoundCloud playlist ID storage.

## Requirements

### Requirement: Publication targets table
The system SHALL store per-podcast publication target configuration in a `podcast_publication_targets` table with columns: `id` (INTEGER, auto-increment PK), `podcast_id` (TEXT, FK to podcasts with ON DELETE CASCADE, NOT NULL), `target` (TEXT, NOT NULL), `config` (TEXT, NOT NULL, default `'{}'`), `enabled` (INTEGER, NOT NULL, default `0`). A unique constraint SHALL exist on `(podcast_id, target)`. The `config` column SHALL store a JSON object with target-specific settings. Known config shapes: SoundCloud `{"playlistId": "..."}`, FTP `{"remotePath": "/path/", "publicUrl": "https://example.com/path/"}`.

#### Scenario: Table created by migration
- **WHEN** the Flyway migration runs
- **THEN** the `podcast_publication_targets` table is created with all specified columns, constraints, and defaults

#### Scenario: Unique constraint prevents duplicate targets
- **WHEN** an INSERT is attempted for a `(podcast_id, target)` pair that already exists
- **THEN** the database rejects the insert with a unique constraint violation

#### Scenario: Cascade delete removes targets when podcast deleted
- **WHEN** a podcast with 2 publication targets is deleted
- **THEN** both `podcast_publication_targets` records are automatically removed via cascade

### Requirement: Publication target entity and repository
The system SHALL define a `PodcastPublicationTarget` entity mapped to the `podcast_publication_targets` table with fields: `id` (Long?), `podcastId` (String), `target` (String), `config` (String, default `"{}"`), `enabled` (Boolean, default `false`). The `PodcastPublicationTargetRepository` SHALL provide: `findByPodcastId(podcastId)` returning all targets for a podcast, `findByPodcastIdAndTarget(podcastId, target)` returning a specific target, and `deleteByPodcastIdAndTarget(podcastId, target)` for removal.

#### Scenario: Find all targets for a podcast
- **WHEN** `findByPodcastId` is called for a podcast with 2 targets (soundcloud, ftp)
- **THEN** both `PodcastPublicationTarget` records are returned

#### Scenario: Find specific target
- **WHEN** `findByPodcastIdAndTarget(podcastId, "ftp")` is called and an FTP target exists
- **THEN** the FTP target record is returned

#### Scenario: Find specific target that does not exist
- **WHEN** `findByPodcastIdAndTarget(podcastId, "ftp")` is called and no FTP target exists
- **THEN** null is returned

### Requirement: CRUD API for publication targets
The system SHALL provide REST endpoints for managing per-podcast publication targets:
- `GET /users/{userId}/podcasts/{podcastId}/publication-targets` -- list all targets for the podcast
- `PUT /users/{userId}/podcasts/{podcastId}/publication-targets/{target}` -- create or update a target (request body: `{"config": {...}, "enabled": true/false}`)
- `DELETE /users/{userId}/podcasts/{podcastId}/publication-targets/{target}` -- remove a target

The list endpoint SHALL return targets with their `target`, `config` (parsed as JSON object), and `enabled` status. The PUT endpoint SHALL upsert: create if not exists, update if exists.

#### Scenario: List targets for podcast with two targets
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}/publication-targets` request is received for a podcast with SoundCloud and FTP targets
- **THEN** HTTP 200 is returned with a JSON array containing both targets with their config and enabled status

#### Scenario: List targets for podcast with no targets
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}/publication-targets` request is received for a podcast with no targets configured
- **THEN** HTTP 200 is returned with an empty JSON array

#### Scenario: Create a new FTP target
- **WHEN** a `PUT /users/{userId}/podcasts/{podcastId}/publication-targets/ftp` request is received with `{"config": {"remotePath": "/shows/tech/"}, "enabled": true}`
- **THEN** the target is created and HTTP 200 is returned with the saved target

#### Scenario: Update existing target config
- **WHEN** a `PUT .../publication-targets/ftp` request is received for an existing FTP target with updated `remotePath`
- **THEN** the existing target's config and enabled status are updated

#### Scenario: Delete a target
- **WHEN** a `DELETE /users/{userId}/podcasts/{podcastId}/publication-targets/ftp` request is received for an existing target
- **THEN** the target is removed and HTTP 204 is returned

#### Scenario: Delete non-existing target
- **WHEN** a `DELETE .../publication-targets/ftp` request is received for a target that does not exist
- **THEN** HTTP 404 is returned

#### Scenario: Podcast not found
- **WHEN** any publication target endpoint is called with a podcast ID that does not exist or does not belong to the user
- **THEN** HTTP 404 is returned

### Requirement: Migrate soundcloudPlaylistId to publication targets
A Flyway migration SHALL migrate existing `soundcloudPlaylistId` data from the `podcasts` table into `podcast_publication_targets`. For each podcast where `soundcloud_playlist_id` is not null, the migration SHALL insert a row with `target = 'soundcloud'`, `config = '{"playlistId": "<value>"}'`, and `enabled = 1`. After migration, the `soundcloud_playlist_id` column SHALL be dropped from the `podcasts` table (SQLite requires table recreation).

#### Scenario: Podcast with existing playlist ID
- **WHEN** the migration runs and a podcast has `soundcloud_playlist_id = '12345'`
- **THEN** a `podcast_publication_targets` row is created with `target = 'soundcloud'`, `config = '{"playlistId":"12345"}'`, `enabled = 1`

#### Scenario: Podcast without playlist ID
- **WHEN** the migration runs and a podcast has `soundcloud_playlist_id = NULL`
- **THEN** no `podcast_publication_targets` row is created for that podcast

#### Scenario: Column dropped after migration
- **WHEN** the migration completes
- **THEN** the `podcasts` table no longer has a `soundcloud_playlist_id` column

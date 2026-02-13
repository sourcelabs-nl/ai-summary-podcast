# Capability: Content Store

## Purpose

Persistent storage for sources, articles, and episodes using SQLite with Spring Data JDBC, including deduplication and schema initialization.

## Requirements

### Requirement: Source persistence
The system SHALL persist source polling state in a `sources` SQLite table with columns: `id` (text, primary key), `type` (text), `url` (text), `last_polled` (timestamp, nullable), and `last_seen_id` (text, nullable). Spring Data JDBC repositories SHALL provide CRUD operations.

#### Scenario: Source polling state updated after successful poll
- **WHEN** a source is polled successfully
- **THEN** the `last_polled` timestamp and `last_seen_id` are updated in the database

#### Scenario: First poll of a new source
- **WHEN** a source has no entry in the database yet
- **THEN** an entry is created with `last_polled` and `last_seen_id` set to null, and all discovered articles are treated as new

### Requirement: Article persistence with deduplication
The system SHALL persist articles in an `articles` SQLite table with columns: `id` (auto-generated), `source_id` (text, foreign key), `title` (text), `body` (text), `url` (text), `published_at` (timestamp, nullable), `content_hash` (text, unique), `is_relevant` (boolean, nullable), and `is_processed` (boolean, default false). Deduplication SHALL be enforced via a unique constraint on `content_hash`.

#### Scenario: New article stored successfully
- **WHEN** an article with a unique content hash is saved
- **THEN** the article is persisted with `is_relevant` = null and `is_processed` = false

#### Scenario: Duplicate article rejected
- **WHEN** an article with an already-existing content hash is saved
- **THEN** the duplicate is silently ignored and the existing record remains unchanged

#### Scenario: Article marked as relevant after filtering
- **WHEN** the LLM relevance filter scores an article at 3 or above
- **THEN** the article's `is_relevant` field is set to true

#### Scenario: Article marked as processed after briefing generation
- **WHEN** an article's summary has been included in a generated briefing
- **THEN** the article's `is_processed` field is set to true

### Requirement: Episode persistence
The system SHALL persist episodes in an `episodes` SQLite table with columns: `id` (auto-generated), `podcast_id` (TEXT, FK to podcasts, NOT NULL), `generated_at` (TEXT, NOT NULL), `script_text` (TEXT, NOT NULL), `status` (TEXT, NOT NULL, default `GENERATED`), `audio_file_path` (TEXT, nullable), and `duration_seconds` (INTEGER, nullable). The `status` column tracks the episode lifecycle (`PENDING_REVIEW`, `APPROVED`, `GENERATED`, `FAILED`, `DISCARDED`). Episodes with status `PENDING_REVIEW` or `APPROVED` SHALL have null `audio_file_path` and `duration_seconds`.

#### Scenario: Episode stored after successful generation (no review)
- **WHEN** the TTS pipeline completes and produces an MP3 file for a podcast with `requireReview = false`
- **THEN** an episode record is created with status `GENERATED`, the script text, audio file path, and calculated duration

#### Scenario: Episode stored as pending review
- **WHEN** the LLM pipeline completes for a podcast with `requireReview = true`
- **THEN** an episode record is created with status `PENDING_REVIEW`, the script text populated, and `audio_file_path` and `duration_seconds` set to null

#### Scenario: Episode status updated after TTS completion
- **WHEN** the async TTS pipeline completes for an approved episode
- **THEN** the episode's `status` is updated to `GENERATED` and `audio_file_path` and `duration_seconds` are populated

#### Scenario: Existing episodes retain GENERATED status after migration
- **WHEN** the V5 migration runs on a database with existing episodes
- **THEN** all existing episodes receive status `GENERATED` and their `audio_file_path` and `duration_seconds` values are preserved

### Requirement: SQLite database initialization
The system SHALL initialize the SQLite database schema using Flyway versioned migrations instead of Spring SQL init (`schema.sql`). Flyway SHALL create all required tables on first startup via the `V1__baseline.sql` migration. The database SHALL use WAL mode for improved read concurrency.

#### Scenario: First startup creates schema
- **WHEN** the application starts and no database file exists
- **THEN** Flyway creates the database file and applies `V1__baseline.sql` to create all tables

#### Scenario: Subsequent startup preserves data
- **WHEN** the application starts and the database already exists with data
- **THEN** existing data is preserved and only pending migrations (if any) are applied

#### Scenario: Upgrade from schema.sql-based initialization
- **WHEN** the application starts on a database previously initialized by `schema.sql` (no `flyway_schema_history` table)
- **THEN** Flyway baselines the database and applies `V1__baseline.sql` without data loss

### Requirement: Test database isolation
Tests SHALL use a separate in-memory SQLite database (`jdbc:sqlite:file:testdb?mode=memory&cache=shared`) via a test-specific `src/test/resources/application.yaml`. Tests SHALL NEVER connect to the production database file. The test configuration SHALL also use a dummy encryption master key and a separate episodes directory.

#### Scenario: Spring Boot tests use in-memory database
- **WHEN** a `@SpringBootTest` test runs
- **THEN** it connects to the in-memory SQLite database, not the production file at `./data/ai-summary-podcast.db`

#### Scenario: Test data does not persist between test runs
- **WHEN** the test suite completes
- **THEN** all test data is discarded with the in-memory database

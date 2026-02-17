## MODIFIED Requirements

### Requirement: Source persistence
The system SHALL persist source polling state in a `sources` SQLite table with columns: `id` (text, primary key), `type` (text), `url` (text), `last_polled` (timestamp, nullable), `last_seen_id` (text, nullable), and `created_at` (TEXT, NOT NULL, ISO-8601 timestamp of when the source was added). Spring Data JDBC repositories SHALL provide CRUD operations.

#### Scenario: Source polling state updated after successful poll
- **WHEN** a source is polled successfully
- **THEN** the `last_polled` timestamp and `last_seen_id` are updated in the database

#### Scenario: First poll of a new source
- **WHEN** a source has no entry in the database yet
- **THEN** an entry is created with `last_polled` and `last_seen_id` set to null, `created_at` set to the current timestamp, and all discovered articles are treated as new

#### Scenario: Existing sources after migration
- **WHEN** the migration adding `created_at` runs on a database with existing sources
- **THEN** existing sources receive `created_at` = `'1970-01-01T00:00:00Z'` so they continue to ingest all content

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

#### Scenario: Episode-articles migration applied
- **WHEN** the migration for `episode_articles` is applied
- **THEN** the `episode_articles` table is created with columns `id`, `episode_id`, `article_id`, and a unique constraint on `(episode_id, article_id)`

#### Scenario: Source created_at migration applied
- **WHEN** the migration for `sources.created_at` is applied
- **THEN** the `created_at` column is added to the `sources` table with default value `'1970-01-01T00:00:00Z'` for existing rows

# Capability: Database Migrations

## Purpose

Versioned database schema management using Flyway, covering migration file conventions, baseline setup, and auto-configuration with Spring Boot.

## Requirements

### Requirement: Flyway dependency and auto-configuration
The system SHALL include `spring-boot-starter-flyway` as a Maven dependency. Flyway SHALL be auto-configured by Spring Boot using the application's existing `DataSource` bean with no additional Flyway bean configuration.

#### Scenario: Flyway runs on application startup
- **WHEN** the application starts
- **THEN** Flyway executes all pending migrations from `db/migration/` before the application accepts requests

#### Scenario: No pending migrations
- **WHEN** the application starts and all migrations have already been applied
- **THEN** Flyway completes without applying any changes and the application starts normally

### Requirement: Baseline migration for existing databases
The system SHALL configure `spring.flyway.baseline-on-migrate=true` with `spring.flyway.baseline-version=0`. This ensures existing databases (previously initialized by `schema.sql`) are baselined automatically on first Flyway-managed startup.

#### Scenario: First Flyway startup on existing database
- **WHEN** the application starts with Flyway for the first time on a database that already has tables but no `flyway_schema_history` table
- **THEN** Flyway creates the `flyway_schema_history` table, records a baseline entry at version 0, and applies all migrations from V1 onward

#### Scenario: First Flyway startup on fresh database
- **WHEN** the application starts with Flyway for the first time on an empty database
- **THEN** Flyway creates the `flyway_schema_history` table and applies all migrations starting from V1

### Requirement: V1 baseline migration file
The system SHALL include a migration file `V1__baseline.sql` in `src/main/resources/db/migration/` containing `CREATE TABLE IF NOT EXISTS` statements for all current tables: `users`, `user_api_keys`, `podcasts`, `sources`, `articles`, and `episodes`. The `IF NOT EXISTS` clauses ensure safe execution on both fresh and existing databases.

#### Scenario: V1 migration on fresh database
- **WHEN** Flyway applies `V1__baseline.sql` on an empty database
- **THEN** all 6 tables are created with the correct schema

#### Scenario: V1 migration on existing database
- **WHEN** Flyway applies `V1__baseline.sql` on a database where tables already exist
- **THEN** the `CREATE TABLE IF NOT EXISTS` statements complete without error and existing data is preserved

### Requirement: Migration file conventions
Migration files SHALL follow Flyway's naming convention: `V{version}__{description}.sql` where version is an incrementing integer and description uses underscores. All migration files SHALL be placed in `src/main/resources/db/migration/`.

#### Scenario: New migration added for future schema change
- **WHEN** a developer creates a new migration file `V2__add_some_column.sql` in `db/migration/`
- **THEN** Flyway applies it on the next application startup after V1 has been applied

### Requirement: V11 migration adds token tracking to LLM cache
The system SHALL include a migration file `V11__add_cache_token_tracking.sql` that adds `input_tokens` (INTEGER, nullable) and `output_tokens` (INTEGER, nullable) columns to the `llm_cache` table. These columns store the token usage from the original LLM response so that cache hits can reconstruct accurate usage metadata.

#### Scenario: V11 migration adds token columns
- **WHEN** Flyway applies `V11__add_cache_token_tracking.sql`
- **THEN** the `llm_cache` table has `input_tokens` and `output_tokens` columns, both nullable integers

#### Scenario: Existing cache entries have null tokens after migration
- **WHEN** V11 is applied to a database with existing `llm_cache` entries
- **THEN** those entries have `input_tokens = NULL` and `output_tokens = NULL`

### Requirement: V12 migration adds author column to articles
The system SHALL include a migration file `V12__add_article_author.sql` that adds an `author` column (TEXT, nullable) to the `articles` table. This column stores the article author's name as extracted from RSS feed metadata or website HTML meta tags.

#### Scenario: V12 migration adds author column
- **WHEN** Flyway applies `V12__add_article_author.sql`
- **THEN** the `articles` table has an `author` column of type TEXT, nullable

#### Scenario: Existing articles have null author after migration
- **WHEN** V12 is applied to a database with existing articles
- **THEN** those articles have `author = NULL`

### Requirement: Removal of Spring SQL init
The system SHALL NOT use `spring.sql.init.mode` for schema initialization. The `schema.sql` file SHALL be removed from `src/main/resources/`. Flyway is the sole mechanism for schema management.

#### Scenario: Application starts without schema.sql
- **WHEN** the application starts
- **THEN** Spring does not attempt to run any SQL init scripts and Flyway handles all schema initialization

### Requirement: V13 migration adds OAuth connections table
The system SHALL include a migration file `V13__add_oauth_connections.sql` that creates the `oauth_connections` table with columns: `id` (INTEGER PRIMARY KEY AUTOINCREMENT), `user_id` (TEXT NOT NULL, FK to users), `provider` (TEXT NOT NULL), `encrypted_access_token` (TEXT NOT NULL), `encrypted_refresh_token` (TEXT), `expires_at` (TEXT), `scopes` (TEXT), `created_at` (TEXT NOT NULL), `updated_at` (TEXT NOT NULL). A unique constraint SHALL exist on `(user_id, provider)`.

#### Scenario: V13 migration creates oauth_connections table
- **WHEN** Flyway applies `V13__add_oauth_connections.sql`
- **THEN** the `oauth_connections` table exists with all specified columns and the unique constraint on `(user_id, provider)`

#### Scenario: V13 migration on fresh database
- **WHEN** Flyway applies V13 on a database with no prior data
- **THEN** the migration completes successfully and the table is empty

### Requirement: V14 migration adds episode publications table
The system SHALL include a migration file `V14__add_episode_publications.sql` that creates the `episode_publications` table with columns: `id` (INTEGER PRIMARY KEY AUTOINCREMENT), `episode_id` (INTEGER NOT NULL, FK to episodes), `target` (TEXT NOT NULL), `status` (TEXT NOT NULL), `external_id` (TEXT), `external_url` (TEXT), `error_message` (TEXT), `published_at` (TEXT), `created_at` (TEXT NOT NULL). A unique constraint SHALL exist on `(episode_id, target)`.

#### Scenario: V14 migration creates episode_publications table
- **WHEN** Flyway applies `V14__add_episode_publications.sql`
- **THEN** the `episode_publications` table exists with all specified columns and the unique constraint on `(episode_id, target)`

#### Scenario: V14 migration on fresh database
- **WHEN** Flyway applies V14 on a database with no prior data
- **THEN** the migration completes successfully and the table is empty

### Requirement: Foreign key enforcement at connection level
The system SHALL enable SQLite foreign key enforcement by configuring HikariCP's `connection-init-sql` to execute `PRAGMA foreign_keys = ON` on every new database connection. This ensures referential integrity is enforced for all database operations, not just those that explicitly enable it.

#### Scenario: Foreign keys enforced on new connection
- **WHEN** a new database connection is obtained from the connection pool
- **THEN** `PRAGMA foreign_keys = ON` has been executed and foreign key constraints are active

#### Scenario: Delete parent row with child references
- **WHEN** a row in a parent table is deleted and child rows reference it via a foreign key without `ON DELETE CASCADE`
- **THEN** the delete fails with a foreign key constraint violation

### Requirement: V30 migration adds indexes
The system SHALL include a migration file `V30__add_indexes.sql` that adds indexes to frequently queried columns across multiple tables. The indexes SHALL include: `idx_articles_podcast_processed` on `articles(podcast_id, is_processed)`, `idx_articles_published_at` on `articles(published_at)`, `idx_articles_source_hash` on `articles(source_id, content_hash)`, `idx_episodes_podcast_status` on `episodes(podcast_id, status)`, `idx_episodes_created_at` on `episodes(created_at)`, `idx_posts_source_hash` on `posts(source_id, content_hash)`, `idx_posts_created_at` on `posts(created_at)`, `idx_sources_podcast_enabled` on `sources(podcast_id, enabled)`, `idx_episode_articles_episode` on `episode_articles(episode_id)`, and `idx_llm_cache_key` on `llm_cache(cache_key)`. All indexes SHALL use `CREATE INDEX IF NOT EXISTS` for idempotent application.

#### Scenario: V30 migration adds performance indexes
- **WHEN** Flyway applies `V30__add_indexes.sql`
- **THEN** all 10 indexes are created on the respective tables

#### Scenario: V30 migration is idempotent
- **WHEN** V30 is applied on a database that already has some of these indexes
- **THEN** the `IF NOT EXISTS` clause prevents errors and the migration completes successfully

### Requirement: V31 migration adds cascade deletes on join tables
The system SHALL include a migration file `V31__add_cascade_deletes.sql` that recreates the `post_articles` and `episode_articles` join tables with `ON DELETE CASCADE` foreign keys. Because SQLite does not support `ALTER CONSTRAINT`, the migration SHALL: (1) clean up any orphaned rows in the join tables, (2) create new tables with cascade delete foreign keys, (3) copy data from old to new tables, (4) drop old tables, (5) rename new tables. The migration SHALL also re-add the `idx_episode_articles_episode` index that is dropped with the old table.

#### Scenario: V31 migration enables cascade deletes
- **WHEN** Flyway applies `V31__add_cascade_deletes.sql`
- **THEN** deleting an article cascades to remove related rows in `post_articles` and `episode_articles`

#### Scenario: V31 migration cleans orphaned rows
- **WHEN** V31 is applied and orphaned rows exist in join tables (referencing deleted parents)
- **THEN** the orphaned rows are deleted before the new FK constraints are applied

#### Scenario: Article deletion cascades to join tables
- **WHEN** an article is deleted from the `articles` table
- **THEN** all corresponding rows in `post_articles` and `episode_articles` are automatically deleted

### Requirement: V32 migration adds cascade delete on episode publications
The system SHALL include a migration file `V32__add_cascade_delete_episode_publications.sql` that recreates the `episode_publications` table with `ON DELETE CASCADE` on the `episode_id` foreign key. Because SQLite does not support `ALTER CONSTRAINT`, the migration SHALL: (1) clean up any orphaned rows referencing deleted episodes, (2) create a new table with the cascade delete foreign key, (3) copy data from old to new table, (4) drop old table, (5) rename new table. This ensures that deleting an episode automatically removes its publication records without requiring manual cleanup in application code.

#### Scenario: V32 migration enables cascade delete on publications
- **WHEN** Flyway applies `V32__add_cascade_delete_episode_publications.sql`
- **THEN** deleting an episode cascades to remove related rows in `episode_publications`

#### Scenario: V32 migration cleans orphaned rows
- **WHEN** V32 is applied and orphaned publication rows exist (referencing deleted episodes)
- **THEN** the orphaned rows are deleted before the new FK constraint is applied

#### Scenario: Episode deletion cascades to publications
- **WHEN** an episode with publication records is deleted
- **THEN** all corresponding rows in `episode_publications` are automatically deleted

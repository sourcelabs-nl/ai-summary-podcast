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

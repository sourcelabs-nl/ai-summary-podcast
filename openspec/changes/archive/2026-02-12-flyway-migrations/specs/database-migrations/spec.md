## ADDED Requirements

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

### Requirement: Removal of Spring SQL init
The system SHALL NOT use `spring.sql.init.mode` for schema initialization. The `schema.sql` file SHALL be removed from `src/main/resources/`. Flyway is the sole mechanism for schema management.

#### Scenario: Application starts without schema.sql
- **WHEN** the application starts
- **THEN** Spring does not attempt to run any SQL init scripts and Flyway handles all schema initialization

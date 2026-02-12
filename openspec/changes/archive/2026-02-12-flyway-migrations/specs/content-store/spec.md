## MODIFIED Requirements

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

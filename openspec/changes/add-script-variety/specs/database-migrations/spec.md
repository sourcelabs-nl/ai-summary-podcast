## ADDED Requirements

### Requirement: Migration adds podcast compose_temperature column

A new Flyway migration (next available `V` number, e.g. `V53__add_podcast_compose_temperature.sql`) SHALL add column `compose_temperature REAL` (nullable, no default) to the `podcasts` table.

#### Scenario: Existing podcasts get null values

- **WHEN** the migration runs against a database with existing rows in `podcasts`
- **THEN** every existing row has `compose_temperature IS NULL`

#### Scenario: Migration is idempotent under Flyway

- **WHEN** the application starts after the migration has already been applied
- **THEN** Flyway does not re-run it

## Why

The application initializes its database using `spring.sql.init.mode=always` with a static `schema.sql` containing `CREATE TABLE IF NOT EXISTS` statements. This approach cannot handle schema evolution — adding columns, renaming fields, or changing constraints on existing tables requires manual intervention or database recreation with data loss. As the application grows, a proper migration tool is needed to apply incremental, versioned schema changes safely and repeatably.

## What Changes

- Add Flyway as a dependency for versioned database migration management
- Convert the existing `schema.sql` into a Flyway baseline migration (V1)
- Remove `spring.sql.init.mode=always` configuration — Flyway takes over schema initialization
- Remove `schema.sql` from `src/main/resources` (replaced by Flyway migration scripts)
- Configure Flyway for SQLite in `application.yaml`
- Establish a migration directory structure (`db/migration/`) for future schema changes

## Capabilities

### New Capabilities
- `database-migrations`: Versioned database schema management using Flyway, covering migration file conventions, baseline setup, and configuration

### Modified Capabilities
- `content-store`: Schema initialization changes from Spring SQL init to Flyway-managed migrations

## Impact

- **Dependencies**: New Maven dependency `flyway-core` (and SQLite Flyway dialect if needed)
- **Configuration**: `application.yaml` changes — remove `spring.sql.init`, add Flyway config
- **Schema files**: `schema.sql` removed, replaced by `db/migration/V1__baseline.sql`
- **Existing databases**: Flyway baseline must handle databases already initialized by `schema.sql` without re-creating tables
- **Startup behavior**: Flyway runs migrations automatically on application start (same as current `schema.sql` behavior, but versioned)
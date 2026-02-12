## Context

The application uses Spring Boot 4.0.2 with SQLite (via `org.xerial:sqlite-jdbc`) and Spring Data JDBC. Database initialization is handled by `spring.sql.init.mode=always` with a `schema.sql` file containing `CREATE TABLE IF NOT EXISTS` statements for 6 tables: `users`, `user_api_keys`, `podcasts`, `sources`, `articles`, and `episodes`.

This approach works for initial setup but cannot evolve the schema — adding columns, changing constraints, or altering tables requires manual SQL or recreating the database. As the application grows (e.g., the upcoming Flyway migration itself is motivated by need for safe schema evolution), a versioned migration tool is needed.

## Goals / Non-Goals

**Goals:**
- Replace `spring.sql.init` with Flyway-managed versioned migrations
- Convert the existing `schema.sql` into a Flyway V1 baseline migration
- Handle existing databases that were initialized by `schema.sql` without data loss
- Establish conventions for future migration files
- Use Spring Boot 4's `spring-boot-starter-flyway` for auto-configuration

**Non-Goals:**
- Migrating from SQLite to another database (PostgreSQL, etc.)
- Adding a Flyway Maven plugin for CLI-based migrations
- Implementing rollback/undo migrations (Flyway community edition doesn't support this)
- Changing any table structure in this change — the schema stays identical

## Decisions

### 1. Use `spring-boot-starter-flyway` for auto-configuration

Spring Boot 4 provides `spring-boot-starter-flyway` which auto-configures Flyway using the application's `DataSource`. This avoids manual Flyway bean configuration and follows Spring Boot conventions.

**Dependencies:**
- `org.springframework.boot:spring-boot-starter-flyway` (managed by Spring Boot BOM)
- No additional `flyway-database-*` module needed — Flyway's core includes SQLite support via the JDBC driver already on the classpath

**Alternative considered:** Adding `flyway-core` directly. Rejected — the starter provides auto-configuration out of the box, reducing boilerplate.

### 2. Baseline migration with `baselineOnMigrate`

Existing databases already have all tables created by `schema.sql`. To avoid Flyway failing on these databases (because the schema already exists but Flyway has no history), we use `spring.flyway.baseline-on-migrate=true` with `baseline-version=0`.

This means:
- **Fresh databases**: Flyway runs V1 to create all tables, then records it in `flyway_schema_history`
- **Existing databases**: Flyway baselines at V0 (recording that the DB is at "version 0"), then runs V1 which uses `CREATE TABLE IF NOT EXISTS` — safe for both cases

**Alternative considered:** Using `flyway baseline` as a separate manual step. Rejected — `baseline-on-migrate` handles this automatically, which is better for a self-hosted app that should just work.

### 3. Migration file location: `db/migration`

Migration files go in `src/main/resources/db/migration/`, which is Flyway's default location. No custom configuration needed.

Naming convention: `V{version}__{description}.sql` (e.g., `V1__baseline.sql`, `V2__add_column_x.sql`).

### 4. V1 migration = current schema.sql content

The first migration (`V1__baseline.sql`) contains the exact content of the current `schema.sql` — all `CREATE TABLE IF NOT EXISTS` statements. The `IF NOT EXISTS` clauses ensure this migration is safe to run on both fresh and existing databases.

### 5. Remove `spring.sql.init` configuration

Once Flyway manages schema initialization, `spring.sql.init.mode=always` must be removed. Keeping both would cause Spring to run `schema.sql` before Flyway, leading to conflicts. The `schema.sql` file itself is also removed.

### 6. SQLite-specific considerations

- **No concurrent migrations**: SQLite doesn't support `SELECT ... FOR UPDATE`. This is fine — the app is single-instance.
- **No transaction wrapping in migrations**: SQLite doesn't support nested transactions. Each DDL statement in a migration runs independently. This is the default behavior.
- **WAL mode**: Not set by Flyway. If WAL mode is needed, it should be configured at the datasource level (e.g., via a connection init SQL), not in migrations.

## Risks / Trade-offs

- **Flyway schema history table**: Flyway creates a `flyway_schema_history` table in the database. This is a minor addition with no operational impact. → No mitigation needed.
- **Existing databases without Flyway history**: On first startup after this change, Flyway needs to baseline. → Mitigated by `baseline-on-migrate=true` with `baseline-version=0`.
- **`CREATE TABLE IF NOT EXISTS` in V1**: Slightly unusual for a Flyway migration (normally migrations assume a clean state). → Acceptable trade-off for safe migration of existing databases. Future migrations (V2+) can assume Flyway manages state normally.
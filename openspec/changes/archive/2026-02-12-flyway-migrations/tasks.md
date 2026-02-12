## 1. Add Flyway dependency

- [x] 1.1 Add `spring-boot-starter-flyway` dependency to `pom.xml` (version managed by Spring Boot BOM)

## 2. Create baseline migration

- [x] 2.1 Create directory `src/main/resources/db/migration/`
- [x] 2.2 Create `V1__baseline.sql` with the contents of current `schema.sql` (all `CREATE TABLE IF NOT EXISTS` statements)

## 3. Configure Flyway in application.yaml

- [x] 3.1 Add `spring.flyway.baseline-on-migrate: true` to `application.yaml`
- [x] 3.2 Add `spring.flyway.baseline-version: 0` to `application.yaml`
- [x] 3.3 Remove `spring.sql.init.mode: always` from `application.yaml`

## 4. Remove schema.sql

- [x] 4.1 Delete `src/main/resources/schema.sql`

## 5. Verify

- [x] 5.1 Start application against a fresh database (no existing DB file) and verify all tables are created
- [x] 5.2 Start application against an existing database (previously initialized by `schema.sql`) and verify Flyway baselines and migrates without data loss

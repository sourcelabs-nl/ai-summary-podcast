## ADDED Requirements

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

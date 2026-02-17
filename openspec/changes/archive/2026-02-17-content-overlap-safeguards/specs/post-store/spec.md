## MODIFIED Requirements

### Requirement: Post persistence with deduplication
The system SHALL persist individual content items in a `posts` SQLite table with columns: `id` (auto-generated INTEGER PRIMARY KEY), `source_id` (TEXT, NOT NULL, FK to sources), `title` (TEXT, NOT NULL), `body` (TEXT, NOT NULL), `url` (TEXT, NOT NULL), `published_at` (TEXT, nullable), `author` (TEXT, nullable), `content_hash` (TEXT, NOT NULL), `created_at` (TEXT, NOT NULL, ISO-8601 timestamp of when the post was stored). Deduplication SHALL be enforced via a unique constraint on `(source_id, content_hash)`. An index SHALL exist on `(source_id, created_at)` for efficient time-windowed queries. Additionally, the system SHALL provide a cross-source deduplication query to check whether a post with a given content hash already exists across a set of source IDs (all sources within the same podcast).

#### Scenario: New post stored successfully
- **WHEN** a post with a unique content hash for its source is saved
- **THEN** the post is persisted with all fields populated and `created_at` set to the current timestamp

#### Scenario: Duplicate post rejected
- **WHEN** a post with an already-existing `(source_id, content_hash)` combination is saved
- **THEN** the duplicate is silently ignored and the existing record remains unchanged

#### Scenario: Posts from different sources with same content hash
- **WHEN** two posts from different sources have the same content hash
- **THEN** both posts are stored (deduplication is scoped per source)

#### Scenario: Cross-source duplicate detected within same podcast
- **WHEN** source A and source B belong to the same podcast, source A already has a post with content hash "abc123", and source B polls a post with the same content hash "abc123"
- **THEN** the post from source B is silently skipped because a post with the same content hash already exists within the podcast's sources

#### Scenario: Same content hash across different podcasts is allowed
- **WHEN** source A belongs to podcast 1 and source B belongs to podcast 2, and both poll a post with content hash "abc123"
- **THEN** both posts are stored because cross-source dedup is scoped to sources within the same podcast

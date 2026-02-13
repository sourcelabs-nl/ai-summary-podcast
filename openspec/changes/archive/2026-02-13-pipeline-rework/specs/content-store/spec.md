## MODIFIED Requirements

### Requirement: Article persistence with deduplication
The system SHALL persist articles in an `articles` SQLite table with columns: `id` (auto-generated), `source_id` (text, foreign key), `title` (text), `body` (text), `url` (text), `published_at` (timestamp, nullable), `content_hash` (text, unique), `relevance_score` (integer, nullable), `is_processed` (boolean, default false), and `summary` (text, nullable). Deduplication SHALL be enforced via a unique constraint on `content_hash`. The `relevance_score` column stores a 0-10 integer score (null means unscored). Relevance is determined at pipeline runtime by comparing the score against a podcast-specific threshold, not stored as a boolean.

#### Scenario: New article stored successfully
- **WHEN** an article with a unique content hash is saved
- **THEN** the article is persisted with `relevance_score` = null, `summary` = null, and `is_processed` = false

#### Scenario: Duplicate article rejected
- **WHEN** an article with an already-existing content hash is saved
- **THEN** the duplicate is silently ignored and the existing record remains unchanged

#### Scenario: Article scored after relevance check
- **WHEN** the LLM relevance scorer assigns a score of 7 to an article
- **THEN** the article's `relevance_score` field is set to 7

#### Scenario: Article marked as processed after briefing generation
- **WHEN** an article's content has been included in a generated briefing
- **THEN** the article's `is_processed` field is set to true

## REMOVED Requirements

### Requirement: Article marked as relevant after filtering
**Reason**: The `is_relevant` boolean column is replaced by `relevance_score` (integer 0-10). Relevance is now determined at pipeline runtime by comparing the score against a configurable per-podcast threshold.
**Migration**: Replace all references to `is_relevant` with `relevance_score` comparisons. Existing data migrated: `is_relevant = true` → `relevance_score = 5`, `is_relevant = false` → `relevance_score = 0`, `null` → `null`.

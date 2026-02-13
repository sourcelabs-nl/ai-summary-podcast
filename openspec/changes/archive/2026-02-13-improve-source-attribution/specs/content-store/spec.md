## MODIFIED Requirements

### Requirement: Article persistence with deduplication
The system SHALL persist articles in an `articles` SQLite table with columns: `id` (auto-generated), `source_id` (text, foreign key), `title` (text), `body` (text), `url` (text), `published_at` (timestamp, nullable), `content_hash` (text, unique), `relevance_score` (integer, nullable), `is_processed` (boolean, default false), `summary` (text, nullable), `author` (text, nullable), `llm_input_tokens` (integer, nullable), `llm_output_tokens` (integer, nullable), and `llm_cost_cents` (integer, nullable). Deduplication SHALL be enforced via a unique constraint on `content_hash`. The `relevance_score` column stores a 0-10 integer score (null means unscored). Relevance is determined at pipeline runtime by comparing the score against a podcast-specific threshold, not stored as a boolean. The `author` column stores the article author's name as extracted from RSS feed metadata or website meta tags (null when not available). The `llm_input_tokens` and `llm_output_tokens` columns track the total LLM tokens used for processing the article (scoring + summarization). The `llm_cost_cents` column stores the estimated cost in cents (null if pricing is not configured).

#### Scenario: New article stored successfully
- **WHEN** an article with a unique content hash is saved
- **THEN** the article is persisted with `relevance_score` = null, `summary` = null, `author` populated or null, `is_processed` = false, and all cost fields = null

#### Scenario: Duplicate article rejected
- **WHEN** an article with an already-existing content hash is saved
- **THEN** the duplicate is silently ignored and the existing record remains unchanged

#### Scenario: Article scored after relevance check
- **WHEN** the LLM relevance scorer assigns a score of 7 to an article
- **THEN** the article's `relevance_score` field is set to 7

#### Scenario: Article marked as processed after briefing generation
- **WHEN** an article's content has been included in a generated briefing
- **THEN** the article's `is_processed` field is set to true

#### Scenario: Article token counts updated after scoring
- **WHEN** an article is scored using 500 input tokens and 50 output tokens
- **THEN** the article's `llm_input_tokens` is set to 500 and `llm_output_tokens` to 50

#### Scenario: Article token counts accumulated after summarization
- **WHEN** an article already has `llm_input_tokens` = 500 and `llm_output_tokens` = 50 from scoring, and summarization uses 800 input tokens and 100 output tokens
- **THEN** the article's `llm_input_tokens` is updated to 1300 and `llm_output_tokens` to 150

#### Scenario: Existing articles have null author after migration
- **WHEN** the V12 migration is applied to a database with existing articles
- **THEN** all existing articles have `author` = null

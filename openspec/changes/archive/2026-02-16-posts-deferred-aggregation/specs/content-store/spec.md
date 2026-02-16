## MODIFIED Requirements

### Requirement: Article persistence with deduplication
The system SHALL persist articles in an `articles` SQLite table with columns: `id` (auto-generated), `source_id` (text, foreign key), `title` (text), `body` (text), `url` (text), `published_at` (timestamp, nullable), `content_hash` (text), `relevance_score` (integer, nullable), `is_processed` (boolean, default false), `summary` (text, nullable), `author` (text, nullable), `llm_input_tokens` (integer, nullable), `llm_output_tokens` (integer, nullable), and `llm_cost_cents` (integer, nullable). Deduplication SHALL be enforced via a unique constraint on `(source_id, content_hash)`. Articles are no longer created during source polling. Instead, articles SHALL be created at script generation time by the `SourceAggregator` which aggregates posts into articles. The `relevance_score`, `summary`, `is_processed`, and LLM cost fields behave as before — they are populated by the LLM pipeline after article creation.

#### Scenario: Article created from aggregated posts
- **WHEN** the SourceAggregator creates an article from 5 posts at script generation time
- **THEN** the article is persisted with `relevance_score` = null, `summary` = null, `is_processed` = false, and all cost fields = null

#### Scenario: Article created from single post (non-aggregated source)
- **WHEN** the SourceAggregator creates a 1:1 article from a single post
- **THEN** the article is persisted with the post's title, body, url, publishedAt, and author

#### Scenario: Duplicate article rejected
- **WHEN** an article with an already-existing `(source_id, content_hash)` is saved
- **THEN** the duplicate is silently ignored and the existing record remains unchanged

#### Scenario: Article scored after relevance check
- **WHEN** the LLM score+summarize stage assigns a score of 7 to an article
- **THEN** the article's `relevance_score` field is set to 7

#### Scenario: Article marked as processed after briefing generation
- **WHEN** an article's content has been included in a generated briefing
- **THEN** the article's `is_processed` field is set to true

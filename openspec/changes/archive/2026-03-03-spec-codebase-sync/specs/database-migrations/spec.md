## MODIFIED Requirements

### Requirement: V30 migration adds indexes
The system SHALL include a migration file `V30__add_indexes.sql` that adds indexes to frequently queried columns across multiple tables. The indexes SHALL include: `idx_articles_source_score` on `articles(source_id, relevance_score)`, `idx_articles_source_processed_score` on `articles(source_id, is_processed, relevance_score)`, `idx_articles_published_processed` on `articles(published_at, is_processed)`, `idx_episodes_podcast_status` on `episodes(podcast_id, status)`, `idx_episodes_podcast_generated` on `episodes(podcast_id, last_generated_at)`, `idx_posts_hash_source` on `posts(content_hash, source_id)`, `idx_sources_podcast` on `sources(podcast_id)`, `idx_episode_articles_episode` on `episode_articles(episode_id)`, and `idx_llm_cache_created` on `llm_cache(created_at)`.

#### Scenario: V30 migration adds performance indexes
- **WHEN** Flyway applies `V30__add_indexes.sql`
- **THEN** all indexes are created on the respective tables

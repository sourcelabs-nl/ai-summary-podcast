-- Indexes for articles table (grows with every pipeline run)
CREATE INDEX idx_articles_source_score ON articles(source_id, relevance_score);
CREATE INDEX idx_articles_source_processed_score ON articles(source_id, is_processed, relevance_score);
CREATE INDEX idx_articles_published_processed ON articles(published_at, is_processed);

-- Indexes for episodes table (queried on feed requests, scheduler checks, manual generation)
CREATE INDEX idx_episodes_podcast_status ON episodes(podcast_id, status);
CREATE INDEX idx_episodes_podcast_generated ON episodes(podcast_id, generated_at DESC);

-- Indexes for posts table (cross-source dedup during polling)
CREATE INDEX idx_posts_hash_source ON posts(content_hash, source_id);

-- Indexes for sources table (looked up by podcast on every scheduler cycle)
CREATE INDEX idx_sources_podcast ON sources(podcast_id);

-- Indexes for join/link tables
CREATE INDEX idx_episode_articles_episode ON episode_articles(episode_id);

-- Index for llm_cache cleanup
CREATE INDEX idx_llm_cache_created ON llm_cache(created_at);

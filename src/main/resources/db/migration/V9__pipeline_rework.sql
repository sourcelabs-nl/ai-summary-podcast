-- Add relevance_score column to articles
ALTER TABLE articles ADD COLUMN relevance_score INTEGER;

-- Migrate existing is_relevant data to relevance_score
UPDATE articles SET relevance_score = 5 WHERE is_relevant = 1;
UPDATE articles SET relevance_score = 0 WHERE is_relevant = 0;

-- Drop the old is_relevant column
ALTER TABLE articles DROP COLUMN is_relevant;

-- Add relevance_threshold to podcasts
ALTER TABLE podcasts ADD COLUMN relevance_threshold INTEGER NOT NULL DEFAULT 5;

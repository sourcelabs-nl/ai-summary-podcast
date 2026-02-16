ALTER TABLE podcasts ADD COLUMN max_article_age_days INTEGER;
ALTER TABLE sources ADD COLUMN max_failures INTEGER;
ALTER TABLE sources ADD COLUMN max_backoff_hours INTEGER;

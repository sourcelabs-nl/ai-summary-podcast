CREATE INDEX IF NOT EXISTS idx_episode_publications_episode_id ON episode_publications(episode_id);
CREATE INDEX IF NOT EXISTS idx_post_articles_article_id ON post_articles(article_id);
CREATE INDEX IF NOT EXISTS idx_podcasts_user_id ON podcasts(user_id);

CREATE TABLE episode_articles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    episode_id INTEGER NOT NULL REFERENCES episodes(id),
    article_id INTEGER NOT NULL REFERENCES articles(id),
    UNIQUE(episode_id, article_id)
);

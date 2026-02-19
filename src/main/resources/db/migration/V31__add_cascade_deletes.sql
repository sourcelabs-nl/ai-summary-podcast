-- Add ON DELETE CASCADE to post_articles and episode_articles foreign keys.
-- SQLite does not support ALTER CONSTRAINT, so we recreate the tables.

-- Clean up orphaned rows before applying FK constraints
DELETE FROM post_articles WHERE post_id NOT IN (SELECT id FROM posts);
DELETE FROM post_articles WHERE article_id NOT IN (SELECT id FROM articles);
DELETE FROM episode_articles WHERE episode_id NOT IN (SELECT id FROM episodes);
DELETE FROM episode_articles WHERE article_id NOT IN (SELECT id FROM articles);

-- Recreate post_articles with cascading deletes
CREATE TABLE post_articles_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    post_id INTEGER NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    article_id INTEGER NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    UNIQUE(post_id, article_id)
);

INSERT INTO post_articles_new (id, post_id, article_id)
SELECT id, post_id, article_id FROM post_articles;

DROP TABLE post_articles;
ALTER TABLE post_articles_new RENAME TO post_articles;

-- Recreate episode_articles with cascading deletes
CREATE TABLE episode_articles_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    episode_id INTEGER NOT NULL REFERENCES episodes(id) ON DELETE CASCADE,
    article_id INTEGER NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    UNIQUE(episode_id, article_id)
);

INSERT INTO episode_articles_new (id, episode_id, article_id)
SELECT id, episode_id, article_id FROM episode_articles;

DROP TABLE episode_articles;
ALTER TABLE episode_articles_new RENAME TO episode_articles;

-- Re-add index dropped with the old table
CREATE INDEX idx_episode_articles_episode ON episode_articles(episode_id);

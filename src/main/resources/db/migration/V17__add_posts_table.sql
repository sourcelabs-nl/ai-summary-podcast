CREATE TABLE IF NOT EXISTS posts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    source_id TEXT NOT NULL REFERENCES sources(id),
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    url TEXT NOT NULL,
    published_at TEXT,
    author TEXT,
    content_hash TEXT NOT NULL,
    created_at TEXT NOT NULL,
    UNIQUE(source_id, content_hash)
);

CREATE INDEX idx_posts_source_created ON posts(source_id, created_at);

CREATE TABLE IF NOT EXISTS post_articles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    post_id INTEGER NOT NULL REFERENCES posts(id),
    article_id INTEGER NOT NULL REFERENCES articles(id),
    UNIQUE(post_id, article_id)
);

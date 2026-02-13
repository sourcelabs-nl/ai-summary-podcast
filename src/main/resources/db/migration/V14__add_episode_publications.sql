CREATE TABLE IF NOT EXISTS episode_publications (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    episode_id INTEGER NOT NULL REFERENCES episodes(id),
    target TEXT NOT NULL,
    status TEXT NOT NULL,
    external_id TEXT,
    external_url TEXT,
    error_message TEXT,
    published_at TEXT,
    created_at TEXT NOT NULL,
    UNIQUE (episode_id, target)
);

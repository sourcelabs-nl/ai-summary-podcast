CREATE TABLE IF NOT EXISTS sources (
    id TEXT PRIMARY KEY,
    type TEXT NOT NULL,
    url TEXT NOT NULL,
    last_polled TEXT,
    last_seen_id TEXT
);

CREATE TABLE IF NOT EXISTS articles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    source_id TEXT NOT NULL REFERENCES sources(id),
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    url TEXT NOT NULL,
    published_at TEXT,
    content_hash TEXT NOT NULL UNIQUE,
    is_relevant INTEGER,
    is_processed INTEGER NOT NULL DEFAULT 0,
    summary TEXT
);

CREATE TABLE IF NOT EXISTS episodes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    generated_at TEXT NOT NULL,
    script_text TEXT NOT NULL,
    audio_file_path TEXT NOT NULL,
    duration_seconds INTEGER NOT NULL
);

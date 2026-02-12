CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS user_api_keys (
    user_id TEXT NOT NULL REFERENCES users(id),
    provider TEXT NOT NULL,
    encrypted_api_key TEXT NOT NULL,
    PRIMARY KEY (user_id, provider)
);

CREATE TABLE IF NOT EXISTS podcasts (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id),
    name TEXT NOT NULL,
    topic TEXT NOT NULL,
    llm_model TEXT,
    tts_voice TEXT DEFAULT 'nova',
    tts_speed REAL DEFAULT 1.0,
    style TEXT DEFAULT 'news-briefing',
    target_words INTEGER,
    cron TEXT DEFAULT '0 0 6 * * *',
    custom_instructions TEXT,
    last_generated_at TEXT,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS sources (
    id TEXT PRIMARY KEY,
    podcast_id TEXT NOT NULL REFERENCES podcasts(id),
    type TEXT NOT NULL,
    url TEXT NOT NULL,
    poll_interval_minutes INTEGER NOT NULL DEFAULT 60,
    enabled INTEGER NOT NULL DEFAULT 1,
    last_polled TEXT,
    last_seen_id TEXT,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS articles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    source_id TEXT NOT NULL REFERENCES sources(id),
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    url TEXT NOT NULL,
    published_at TEXT,
    content_hash TEXT NOT NULL,
    is_relevant INTEGER,
    is_processed INTEGER NOT NULL DEFAULT 0,
    summary TEXT,
    UNIQUE(source_id, content_hash)
);

CREATE TABLE IF NOT EXISTS episodes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    podcast_id TEXT NOT NULL REFERENCES podcasts(id),
    generated_at TEXT NOT NULL,
    script_text TEXT NOT NULL,
    audio_file_path TEXT NOT NULL,
    duration_seconds INTEGER NOT NULL
);
CREATE TABLE IF NOT EXISTS llm_cache (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    prompt_hash TEXT NOT NULL,
    model       TEXT NOT NULL,
    response    TEXT NOT NULL,
    created_at  TEXT NOT NULL,
    UNIQUE (prompt_hash, model)
);

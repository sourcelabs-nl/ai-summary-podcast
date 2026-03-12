CREATE TABLE podcast_publication_targets (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    podcast_id TEXT NOT NULL REFERENCES podcasts(id) ON DELETE CASCADE,
    target TEXT NOT NULL,
    config TEXT NOT NULL DEFAULT '{}',
    enabled INTEGER NOT NULL DEFAULT 0,
    UNIQUE(podcast_id, target)
);

CREATE INDEX idx_podcast_publication_targets_podcast_id ON podcast_publication_targets(podcast_id);

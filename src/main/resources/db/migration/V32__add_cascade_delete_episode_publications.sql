-- Add ON DELETE CASCADE to episode_publications.episode_id foreign key.
-- SQLite does not support ALTER CONSTRAINT, so we recreate the table.

-- Clean up orphaned rows before applying FK constraints
DELETE FROM episode_publications WHERE episode_id NOT IN (SELECT id FROM episodes);

-- Recreate episode_publications with cascading delete on episode_id
CREATE TABLE episode_publications_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    episode_id INTEGER NOT NULL REFERENCES episodes(id) ON DELETE CASCADE,
    target TEXT NOT NULL,
    status TEXT NOT NULL,
    external_id TEXT,
    external_url TEXT,
    error_message TEXT,
    published_at TEXT,
    created_at TEXT NOT NULL,
    UNIQUE (episode_id, target)
);

INSERT INTO episode_publications_new (id, episode_id, target, status, external_id, external_url, error_message, published_at, created_at)
SELECT id, episode_id, target, status, external_id, external_url, error_message, published_at, created_at FROM episode_publications;

DROP TABLE episode_publications;
ALTER TABLE episode_publications_new RENAME TO episode_publications;

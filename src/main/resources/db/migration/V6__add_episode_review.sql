-- Add status lifecycle to episodes and make audio fields nullable.
-- SQLite does not support ALTER COLUMN, so we recreate the table.

CREATE TABLE episodes_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    podcast_id TEXT NOT NULL REFERENCES podcasts(id),
    generated_at TEXT NOT NULL,
    script_text TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'GENERATED',
    audio_file_path TEXT,
    duration_seconds INTEGER
);

INSERT INTO episodes_new (id, podcast_id, generated_at, script_text, status, audio_file_path, duration_seconds)
SELECT id, podcast_id, generated_at, script_text, 'GENERATED', audio_file_path, duration_seconds
FROM episodes;

DROP TABLE episodes;
ALTER TABLE episodes_new RENAME TO episodes;

-- Add per-podcast opt-in for script review
ALTER TABLE podcasts ADD COLUMN require_review INTEGER NOT NULL DEFAULT 0;

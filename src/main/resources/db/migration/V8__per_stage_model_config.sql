-- Replace single llm_model column on podcasts with llm_models JSON map.
-- SQLite does not support DROP COLUMN, so we recreate the table.

CREATE TABLE podcasts_new (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id),
    name TEXT NOT NULL,
    topic TEXT NOT NULL,
    llm_models TEXT,
    language TEXT DEFAULT 'en',
    tts_voice TEXT DEFAULT 'nova',
    tts_speed REAL DEFAULT 1.0,
    style TEXT DEFAULT 'news-briefing',
    target_words INTEGER,
    cron TEXT DEFAULT '0 0 6 * * *',
    custom_instructions TEXT,
    require_review INTEGER NOT NULL DEFAULT 0,
    last_generated_at TEXT,
    version BIGINT NOT NULL DEFAULT 0
);

INSERT INTO podcasts_new (id, user_id, name, topic, llm_models, language, tts_voice, tts_speed, style, target_words, cron, custom_instructions, require_review, last_generated_at, version)
SELECT id, user_id, name, topic,
    CASE WHEN llm_model IS NOT NULL THEN '{"filter":"' || llm_model || '","compose":"' || llm_model || '"}' ELSE NULL END,
    language, tts_voice, tts_speed, style, target_words, cron, custom_instructions, require_review, last_generated_at, version
FROM podcasts;

DROP TABLE podcasts;
ALTER TABLE podcasts_new RENAME TO podcasts;

-- Add model tracking columns to episodes
ALTER TABLE episodes ADD COLUMN filter_model TEXT;
ALTER TABLE episodes ADD COLUMN compose_model TEXT;

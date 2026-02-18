-- Add tts_provider column, rename tts_voice to tts_voices (JSON map), rename tts_speed to tts_settings (JSON map)
-- SQLite doesn't support ALTER COLUMN, so we recreate the table

CREATE TABLE podcasts_new (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    topic TEXT NOT NULL,
    llm_models TEXT,
    language TEXT NOT NULL DEFAULT 'en',
    tts_provider TEXT NOT NULL DEFAULT 'openai',
    tts_voices TEXT,
    tts_settings TEXT,
    style TEXT NOT NULL DEFAULT 'news-briefing',
    target_words INTEGER,
    cron TEXT NOT NULL DEFAULT '0 0 6 * * *',
    custom_instructions TEXT,
    relevance_threshold INTEGER NOT NULL DEFAULT 5,
    require_review INTEGER NOT NULL DEFAULT 0,
    max_llm_cost_cents INTEGER,
    max_article_age_days INTEGER,
    soundcloud_playlist_id TEXT,
    last_generated_at TEXT,
    version INTEGER
);

INSERT INTO podcasts_new (
    id, user_id, name, topic, llm_models, language,
    tts_provider,
    tts_voices,
    tts_settings,
    style, target_words, cron, custom_instructions,
    relevance_threshold, require_review, max_llm_cost_cents,
    max_article_age_days, soundcloud_playlist_id, last_generated_at, version
)
SELECT
    id, user_id, name, topic, llm_models, language,
    'openai',
    '{"default":"' || COALESCE(tts_voice, 'nova') || '"}',
    '{"speed":' || COALESCE(tts_speed, 1.0) || '}',
    style, target_words, cron, custom_instructions,
    relevance_threshold, require_review, max_llm_cost_cents,
    max_article_age_days, soundcloud_playlist_id, last_generated_at, version
FROM podcasts;

DROP TABLE podcasts;
ALTER TABLE podcasts_new RENAME TO podcasts;

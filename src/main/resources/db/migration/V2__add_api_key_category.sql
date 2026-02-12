-- Add category column (nullable first for backfill)
ALTER TABLE user_api_keys ADD COLUMN category TEXT;

-- Backfill existing rows
UPDATE user_api_keys SET category = 'LLM' WHERE provider = 'openrouter';
UPDATE user_api_keys SET category = 'TTS' WHERE provider = 'openai';

-- Default any remaining rows (safety net)
UPDATE user_api_keys SET category = 'LLM' WHERE category IS NULL;

-- Recreate table with new PK (SQLite doesn't support ALTER PK directly)
CREATE TABLE user_api_keys_new (
    user_id TEXT NOT NULL REFERENCES users(id),
    provider TEXT NOT NULL,
    category TEXT NOT NULL,
    encrypted_api_key TEXT NOT NULL,
    PRIMARY KEY (user_id, category)
);

INSERT INTO user_api_keys_new (user_id, provider, category, encrypted_api_key)
SELECT user_id, provider, category, encrypted_api_key FROM user_api_keys;

DROP TABLE user_api_keys;
ALTER TABLE user_api_keys_new RENAME TO user_api_keys;

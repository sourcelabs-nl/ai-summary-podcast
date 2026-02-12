-- Change PK from (user_id, category) to (user_id, category, provider)
-- to allow multiple providers per category per user.
-- SQLite does not support ALTER TABLE ... DROP PRIMARY KEY, so recreate.

CREATE TABLE user_provider_configs_new (
    user_id TEXT NOT NULL REFERENCES users(id),
    provider TEXT NOT NULL,
    category TEXT NOT NULL,
    base_url TEXT,
    encrypted_api_key TEXT,
    PRIMARY KEY (user_id, category, provider)
);

INSERT INTO user_provider_configs_new (user_id, provider, category, base_url, encrypted_api_key)
SELECT user_id, provider, category, base_url, encrypted_api_key
FROM user_provider_configs;

DROP TABLE user_provider_configs;
ALTER TABLE user_provider_configs_new RENAME TO user_provider_configs;

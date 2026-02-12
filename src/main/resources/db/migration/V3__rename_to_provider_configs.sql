-- Recreate table with new name, nullable encrypted_api_key, and base_url column
CREATE TABLE user_provider_configs (
    user_id TEXT NOT NULL REFERENCES users(id),
    provider TEXT NOT NULL,
    category TEXT NOT NULL,
    base_url TEXT,
    encrypted_api_key TEXT,
    PRIMARY KEY (user_id, category)
);

INSERT INTO user_provider_configs (user_id, provider, category, base_url, encrypted_api_key)
SELECT user_id, provider, category, NULL, encrypted_api_key FROM user_api_keys;

DROP TABLE user_api_keys;

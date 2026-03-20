CREATE INDEX IF NOT EXISTS idx_llm_cache_prompt_hash_model ON llm_cache(prompt_hash, model);

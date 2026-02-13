-- Add cost tracking columns to articles (scoring + summarization)
ALTER TABLE articles ADD COLUMN llm_input_tokens INTEGER;
ALTER TABLE articles ADD COLUMN llm_output_tokens INTEGER;
ALTER TABLE articles ADD COLUMN llm_cost_cents INTEGER;

-- Add cost tracking columns to episodes (composition + TTS)
ALTER TABLE episodes ADD COLUMN llm_input_tokens INTEGER;
ALTER TABLE episodes ADD COLUMN llm_output_tokens INTEGER;
ALTER TABLE episodes ADD COLUMN llm_cost_cents INTEGER;
ALTER TABLE episodes ADD COLUMN tts_characters INTEGER;
ALTER TABLE episodes ADD COLUMN tts_cost_cents INTEGER;

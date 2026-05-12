-- Bump Opus model references from claude-opus-4.6 to claude-opus-4.7.
UPDATE podcasts
SET llm_models = REPLACE(
    llm_models,
    '"anthropic/claude-opus-4.6"',
    '"anthropic/claude-opus-4.7"'
)
WHERE llm_models IS NOT NULL
  AND llm_models LIKE '%anthropic/claude-opus-4.6%';
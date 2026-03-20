-- Migrate podcast llm_models from alias strings to {provider, model} objects.
-- Known alias mappings at time of migration:
--   cheap   -> openrouter / openai/gpt-5.4-nano
--   capable -> openrouter / anthropic/claude-sonnet-4.6
--   opus    -> openrouter / anthropic/claude-opus-4.6

UPDATE podcasts
SET llm_models = REPLACE(
    REPLACE(
        REPLACE(
            llm_models,
            '"cheap"',
            '{"provider":"openrouter","model":"openai/gpt-5.4-nano"}'
        ),
        '"capable"',
        '{"provider":"openrouter","model":"anthropic/claude-sonnet-4.6"}'
    ),
    '"opus"',
    '{"provider":"openrouter","model":"anthropic/claude-opus-4.6"}'
)
WHERE llm_models IS NOT NULL
  AND llm_models != '{}';

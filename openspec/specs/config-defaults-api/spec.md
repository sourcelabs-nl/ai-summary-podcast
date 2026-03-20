## Purpose

Defines the API endpoint for exposing system-level podcast configuration defaults to the frontend.

## Requirements

### Requirement: Config defaults endpoint
The system SHALL provide a `GET /config/defaults` endpoint that returns the system default values for podcast settings derived from application configuration. The response SHALL include: `llmModels` (object with `filter` and `compose` keys, each containing `{provider, model}`), `availableModels` (object with provider keys, each containing a list of `{name, type}` entries), `maxLlmCostCents`, `targetWords`, `fullBodyThreshold`, `maxArticleAgeDays`.

#### Scenario: Successful response with structured defaults
- **WHEN** a GET request is made to `/config/defaults` and defaults are `filter: {provider: openrouter, model: openai/gpt-5.4-nano}`, `compose: {provider: openrouter, model: anthropic/claude-sonnet-4.6}`
- **THEN** the response `llmModels` SHALL contain `{"filter": {"provider": "openrouter", "model": "openai/gpt-5.4-nano"}, "compose": {"provider": "openrouter", "model": "anthropic/claude-sonnet-4.6"}}`

#### Scenario: Available models grouped by provider with type
- **WHEN** a GET request is made to `/config/defaults` and `app.models` contains openrouter (LLM models) and inworld (TTS models)
- **THEN** the response `availableModels` SHALL contain `{"openrouter": [{"name": "openai/gpt-5.4-nano", "type": "llm"}, ...], "inworld": [{"name": "inworld-tts-1.5-max", "type": "tts"}, ...]}`

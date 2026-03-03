## Purpose

Defines the API endpoint for exposing system-level podcast configuration defaults to the frontend.

## Requirements

### Requirement: Config defaults endpoint
The system SHALL provide a `GET /config/defaults` endpoint that returns the system default values for podcast settings derived from application configuration (AppProperties).

#### Scenario: Successful response
- **WHEN** a GET request is made to `/config/defaults`
- **THEN** the response SHALL contain: llmModels (resolved model names per stage), maxLlmCostCents, targetWords, fullBodyThreshold, maxArticleAgeDays

#### Scenario: LLM models resolved from aliases
- **WHEN** the defaults endpoint is called and llm.defaults references aliases (e.g., "cheap", "capable")
- **THEN** the response llmModels map SHALL contain the resolved model names (e.g., "openai/gpt-4o-mini") rather than the alias names

## ADDED Requirements

### Requirement: Named model definitions in application configuration
The system SHALL support defining named LLM models in `application.yaml` under `app.llm.models`. Each named model SHALL have a `provider` field (matching a provider name used in `UserProviderConfig`, e.g., `"openrouter"`, `"ollama"`, `"openai"`) and a `model` field (the model ID to send to the provider API, e.g., `"anthropic/claude-haiku-4.5"`). The named models SHALL be loaded into `AppProperties` at startup as a `Map<String, ModelDefinition>`.

#### Scenario: Multiple models defined
- **WHEN** application.yaml contains `app.llm.models` with entries "cheap" (provider: openrouter, model: anthropic/claude-haiku-4.5) and "capable" (provider: openrouter, model: anthropic/claude-sonnet-4)
- **THEN** `AppProperties.llm.models` contains two `ModelDefinition` entries keyed by "cheap" and "capable"

#### Scenario: Model with different provider
- **WHEN** application.yaml contains a model "local" with provider "ollama" and model "llama3"
- **THEN** the model definition resolves to provider "ollama" and model ID "llama3"

#### Scenario: No models defined
- **WHEN** application.yaml does not define any `app.llm.models` entries
- **THEN** `AppProperties.llm.models` is an empty map

### Requirement: Global stage-to-model defaults
The system SHALL support mapping pipeline stages to named model defaults in `application.yaml` under `app.llm.defaults`. The supported stage names SHALL be `filter` (for article relevance filtering and summarization) and `compose` (for briefing script composition). Each default SHALL reference a named model defined in `app.llm.models`.

#### Scenario: Default stage mappings configured
- **WHEN** application.yaml contains `app.llm.defaults.filter: cheap` and `app.llm.defaults.compose: capable`
- **THEN** the filter stage resolves to the "cheap" model definition and the compose stage resolves to the "capable" model definition

#### Scenario: Default references non-existent model
- **WHEN** application.yaml contains `app.llm.defaults.filter: nonexistent` and no model named "nonexistent" exists in `app.llm.models`
- **THEN** the system SHALL throw an error at pipeline runtime when the filter stage is invoked

### Requirement: Model resolution for a pipeline stage
The system SHALL resolve the model for a given pipeline stage using the following resolution chain: (1) check the podcast's `llm_models` JSON map for a stage-specific override, (2) fall back to the global stage default from `app.llm.defaults`, (3) look up the resulting model name in `app.llm.models`. If the resolved name is not found in the model registry, the system SHALL throw an `IllegalArgumentException` with a message indicating the unknown model name and available model names.

#### Scenario: Podcast override takes precedence
- **WHEN** a podcast has `llm_models` set to `{"compose": "local"}` and the global default for compose is "capable"
- **THEN** the compose stage uses the "local" model definition

#### Scenario: Global default used when no podcast override
- **WHEN** a podcast has `llm_models` set to `null` (or empty map) and the global default for filter is "cheap"
- **THEN** the filter stage uses the "cheap" model definition

#### Scenario: Podcast override for one stage, default for another
- **WHEN** a podcast has `llm_models` set to `{"compose": "local"}` (no filter override) and the global defaults are filter: "cheap", compose: "capable"
- **THEN** the filter stage uses "cheap" and the compose stage uses "local"

#### Scenario: Unknown model name fails loudly
- **WHEN** a podcast has `llm_models` set to `{"filter": "nonexistent"}` and no model named "nonexistent" exists
- **THEN** the system throws an `IllegalArgumentException` with a message like "Unknown model name 'nonexistent'. Available models: [cheap, capable, local]"

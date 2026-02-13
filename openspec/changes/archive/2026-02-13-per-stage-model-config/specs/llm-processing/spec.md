## MODIFIED Requirements

### Requirement: Model switching per LLM call
The system SHALL resolve a named model for each pipeline stage via the model resolution chain (podcast override → global default → registry lookup). The `ArticleProcessor` SHALL resolve the model for stage `"filter"` and create a `ChatClient` using that model's provider and credentials. The `BriefingComposer` SHALL resolve the model for stage `"compose"` and create a `ChatClient` using that model's provider and credentials. Each stage MAY use a different provider and model. The `ChatClientFactory` SHALL accept a user ID and a `ModelDefinition` (provider + model ID) and create a `ChatClient` configured for that specific provider's base URL and the user's credentials for that provider.

#### Scenario: Article processing uses resolved filter model
- **WHEN** the article processing step runs for a podcast whose resolved filter model is "cheap" (provider: openrouter, model: anthropic/claude-haiku-4.5)
- **THEN** the ChatClient is created using the user's openrouter credentials and the call uses model "anthropic/claude-haiku-4.5"

#### Scenario: Briefing composition uses resolved compose model
- **WHEN** the briefing composition step runs for a podcast whose resolved compose model is "capable" (provider: openrouter, model: anthropic/claude-sonnet-4)
- **THEN** the ChatClient is created using the user's openrouter credentials and the call uses model "anthropic/claude-sonnet-4"

#### Scenario: Different providers per stage
- **WHEN** the filter stage resolves to model "cheap" (provider: openrouter) and the compose stage resolves to model "local" (provider: ollama)
- **THEN** two different ChatClient instances are created — one using the user's openrouter config, one using the user's ollama config

#### Scenario: Pipeline fails if model name not in registry
- **WHEN** the pipeline starts and a podcast's resolved model name is not found in the registry
- **THEN** the pipeline fails immediately with an `IllegalArgumentException` before making any LLM calls

## REMOVED Requirements

### Requirement: Combined article processing
**Reason**: The requirement text references "the configured cheap model" which is being replaced by named model resolution. The behavior is unchanged — only the model resolution mechanism changes. The updated behavior is captured in the modified "Model switching per LLM call" requirement above, and the article processing logic itself remains identical.
**Migration**: The `app.llm.cheap-model` config property is replaced by `app.llm.defaults.filter` referencing a named model.

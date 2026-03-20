## MODIFIED Requirements

### Requirement: Settings sub-tabs
The settings form SHALL be organized into four sub-tabs: General, LLM, TTS, and Content. The General tab SHALL be the default active tab.

#### Scenario: LLM tab fields
- **WHEN** the LLM tab is active
- **THEN** the form displays editable fields for: LLM Models (per-stage provider/model dropdowns with defaults shown), relevanceThreshold (number), maxLlmCostCents (number), customInstructions (textarea with 300px min height)

### Requirement: LLM model selector
The LLM Models field SHALL display a per-stage model selector for the `filter` and `compose` pipeline stages. For each stage, the selector SHALL show: a provider dropdown (populated from `availableModels` providers that have at least one LLM-type model), and a model dropdown (populated from the selected provider's LLM-type models from `availableModels`). The system default for each stage SHALL be displayed below the selector (e.g., "Default: openrouter / anthropic/claude-sonnet-4.6"). When no override is set for a stage, the dropdowns SHALL show the default values as placeholder text.

#### Scenario: Display model selector with available models
- **WHEN** the LLM tab is active and `/config/defaults` returns `availableModels` with `openrouter` containing LLM models `openai/gpt-5.4-nano` and `anthropic/claude-sonnet-4.6`
- **THEN** the provider dropdown for each stage lists `openrouter`, and selecting it shows the two LLM models in the model dropdown

#### Scenario: Show defaults when no override set
- **WHEN** the podcast has no `llmModels` override for the `compose` stage and the default is `{provider: openrouter, model: anthropic/claude-sonnet-4.6}`
- **THEN** the compose stage selector shows "Default: openrouter / anthropic/claude-sonnet-4.6" and the dropdowns use the default values as placeholders

#### Scenario: Override a stage model
- **WHEN** the user selects provider `openrouter` and model `anthropic/claude-opus-4.6` for the compose stage
- **THEN** the form stores `llmModels.compose` as `{"provider": "openrouter", "model": "anthropic/claude-opus-4.6"}`

#### Scenario: Clear an override
- **WHEN** the user clears the override for a stage (resets to default)
- **THEN** that stage key is removed from the `llmModels` map

#### Scenario: Only LLM models shown in LLM dropdowns
- **WHEN** `availableModels` contains both LLM and TTS models under various providers
- **THEN** only models with `type: "llm"` appear in the LLM model dropdowns

## REMOVED Requirements

### Requirement: Key-value editor
**Reason**: LLM models field no longer uses a generic key-value editor. It uses dedicated provider/model dropdowns per stage. The key-value editor component remains for other fields (ttsVoices, ttsSettings, etc.).
**Migration**: Replace the key-value editor for `llmModels` with the new LLM model selector component.

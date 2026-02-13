## ADDED Requirements

### Requirement: Cost estimation from token counts
The system SHALL estimate LLM costs in cents using configurable per-model pricing. Each model definition in `app.llm.models` MAY include `input-cost-per-mtok` and `output-cost-per-mtok` (cost per million tokens in USD). The estimated cost in cents SHALL be calculated as: `((inputTokens * inputCostPerMtok + outputTokens * outputCostPerMtok) / 1_000_000 * 100).roundToInt()`. If pricing is not configured for a model, estimated cost SHALL be null.

#### Scenario: Cost estimated with configured pricing
- **WHEN** a model has `input-cost-per-mtok: 0.15` and `output-cost-per-mtok: 0.60`, and an LLM call uses 1000 input tokens and 200 output tokens
- **THEN** estimated cost is `((1000 * 0.15 + 200 * 0.60) / 1_000_000 * 100).roundToInt()` = 0 cents (fractions of a cent round to nearest)

#### Scenario: Cost estimation with larger token counts
- **WHEN** a model has `input-cost-per-mtok: 3.00` and `output-cost-per-mtok: 15.00`, and a composition call uses 10000 input tokens and 2000 output tokens
- **THEN** estimated cost is `((10000 * 3.00 + 2000 * 15.00) / 1_000_000 * 100).roundToInt()` = 6 cents

#### Scenario: No pricing configured
- **WHEN** a model definition does not include `input-cost-per-mtok` or `output-cost-per-mtok`
- **THEN** estimated cost SHALL be null (token counts are still tracked)

### Requirement: TTS cost estimation
The system SHALL estimate TTS costs using a configurable `app.tts.cost-per-million-chars` property (USD per million characters). The estimated TTS cost in cents SHALL be calculated as: `(characters * costPerMillionChars / 1_000_000 * 100).roundToInt()`. If TTS pricing is not configured, estimated cost SHALL be null.

#### Scenario: TTS cost estimated
- **WHEN** `app.tts.cost-per-million-chars` is 15.00 and an episode uses 8000 characters
- **THEN** TTS cost is `(8000 * 15.00 / 1_000_000 * 100).roundToInt()` = 12 cents

#### Scenario: TTS cost for longer script
- **WHEN** `app.tts.cost-per-million-chars` is 15.00 and an episode uses 50000 characters
- **THEN** TTS cost is `(50000 * 15.00 / 1_000_000 * 100).roundToInt()` = 75 cents

#### Scenario: TTS pricing not configured
- **WHEN** `app.tts.cost-per-million-chars` is not set
- **THEN** TTS estimated cost SHALL be null (character count is still tracked)

### Requirement: Cost data exposed in episode API response
The episode API response SHALL include cost fields: `llmInputTokens`, `llmOutputTokens`, `llmCostCents`, `ttsCharacters`, `ttsCostCents`. All cost fields SHALL be nullable (null when data is not available, e.g. for episodes created before cost tracking was added).

#### Scenario: Episode response includes cost data
- **WHEN** an episode is retrieved via `GET /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}`
- **THEN** the response includes `llmInputTokens`, `llmOutputTokens`, `llmCostCents`, `ttsCharacters`, and `ttsCostCents` fields

#### Scenario: Legacy episode without cost data
- **WHEN** an episode created before cost tracking is retrieved
- **THEN** all cost fields are null

### Requirement: Model pricing configuration
Model pricing SHALL be configured in `application.yaml` as optional properties on each model definition under `app.llm.models`. The properties `input-cost-per-mtok` and `output-cost-per-mtok` represent USD per million tokens. TTS pricing SHALL be configured under `app.tts.cost-per-million-chars`.

#### Scenario: Model with pricing configured
- **WHEN** a model definition includes `input-cost-per-mtok: 3.00` and `output-cost-per-mtok: 15.00`
- **THEN** the cost estimator uses these values for that model's cost calculations

#### Scenario: Model without pricing configured
- **WHEN** a model definition omits pricing properties
- **THEN** the cost estimator returns null for estimated cost but token counts are still tracked

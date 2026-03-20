## Requirements

### Requirement: TTS cost estimation
The system SHALL estimate TTS costs by looking up the model's `cost-per-million-chars` from the unified `app.models` configuration. The lookup SHALL use the TTS provider name and model name to find the entry at `app.models.<provider>.<model>`. For Inworld, the model name SHALL come from `ttsSettings["model"]` (defaulting to `inworld-tts-1.5-max`). For OpenAI TTS, the model name SHALL default to `tts-1-hd`. The estimated TTS cost in cents SHALL be calculated as: `(characters * costPerMillionChars / 1_000_000 * 100).roundToInt()`. If TTS pricing is not configured for the provider/model, estimated cost SHALL be null.

#### Scenario: OpenAI TTS cost estimated
- **WHEN** `app.models.openai.tts-1-hd` has `cost-per-million-chars: 15.00` and an episode uses 8000 characters with `ttsProvider: "openai"`
- **THEN** TTS cost is `(8000 * 15.00 / 1_000_000 * 100).roundToInt()` = 12 cents

#### Scenario: Inworld TTS Max cost estimated
- **WHEN** `app.models.inworld.inworld-tts-1.5-max` has `cost-per-million-chars: 10.00` and an episode uses 8000 characters with `ttsProvider: "inworld"` and default model
- **THEN** TTS cost is `(8000 * 10.00 / 1_000_000 * 100).roundToInt()` = 8 cents

#### Scenario: Inworld TTS Mini cost estimated
- **WHEN** `app.models.inworld.inworld-tts-1.5-mini` has `cost-per-million-chars: 5.00` and an episode uses 8000 characters with `ttsProvider: "inworld"` and `ttsSettings: {"model": "inworld-tts-1.5-mini"}`
- **THEN** TTS cost is `(8000 * 5.00 / 1_000_000 * 100).roundToInt()` = 4 cents

#### Scenario: TTS pricing not configured for provider/model
- **WHEN** `app.models` has no entry matching the TTS provider and model
- **THEN** TTS estimated cost SHALL be null (character count is still tracked)

### Requirement: Model pricing configuration
Model pricing SHALL be configured in `application.yaml` under `app.models.<provider>.<model>`. LLM models SHALL use `input-cost-per-mtok` and `output-cost-per-mtok` (USD per million tokens). TTS models SHALL use `cost-per-million-chars` (USD per million characters). All cost fields are optional.

#### Scenario: LLM model with pricing configured
- **WHEN** `app.models.openrouter.anthropic/claude-sonnet-4.6` includes `input-cost-per-mtok: 3.00` and `output-cost-per-mtok: 15.00`
- **THEN** the cost estimator uses these values for that model's cost calculations

#### Scenario: TTS model with pricing configured
- **WHEN** `app.models.inworld.inworld-tts-1.5-max` includes `cost-per-million-chars: 10.00`
- **THEN** the cost estimator uses this value for TTS cost calculations

#### Scenario: Model without pricing configured
- **WHEN** a model entry omits cost fields
- **THEN** the cost estimator returns null for estimated cost but usage counts are still tracked

## MODIFIED Requirements

### Requirement: TTS cost estimation
The system SHALL estimate TTS costs using per-provider cost rates configured under `app.tts.cost-per-million-chars`. The configuration SHALL be a map of provider name to USD per million characters (e.g., `openai: 15.00`, `elevenlabs: 30.00`, `inworld-tts-1.5-max: 10.00`, `inworld-tts-1.5-mini: 5.00`). For Inworld, the cost lookup key SHALL be the model name from `ttsSettings["model"]` (defaulting to `inworld-tts-1.5-max`). The estimated TTS cost in cents SHALL be calculated as: `(characters * costPerMillionChars / 1_000_000 * 100).roundToInt()`, using the rate for the resolved key. If TTS pricing is not configured for the provider/model, estimated cost SHALL be null.

#### Scenario: OpenAI TTS cost estimated
- **WHEN** `app.tts.cost-per-million-chars.openai` is 15.00 and an episode uses 8000 characters with `ttsProvider: "openai"`
- **THEN** TTS cost is `(8000 * 15.00 / 1_000_000 * 100).roundToInt()` = 12 cents

#### Scenario: ElevenLabs TTS cost estimated
- **WHEN** `app.tts.cost-per-million-chars.elevenlabs` is 30.00 and an episode uses 8000 characters with `ttsProvider: "elevenlabs"`
- **THEN** TTS cost is `(8000 * 30.00 / 1_000_000 * 100).roundToInt()` = 24 cents

#### Scenario: Inworld TTS Max cost estimated
- **WHEN** `app.tts.cost-per-million-chars.inworld-tts-1.5-max` is 10.00 and an episode uses 8000 characters with `ttsProvider: "inworld"` and default model
- **THEN** TTS cost is `(8000 * 10.00 / 1_000_000 * 100).roundToInt()` = 8 cents

#### Scenario: Inworld TTS Mini cost estimated
- **WHEN** `app.tts.cost-per-million-chars.inworld-tts-1.5-mini` is 5.00 and an episode uses 8000 characters with `ttsProvider: "inworld"` and `ttsSettings: {"model": "inworld-tts-1.5-mini"}`
- **THEN** TTS cost is `(8000 * 5.00 / 1_000_000 * 100).roundToInt()` = 4 cents

#### Scenario: TTS pricing not configured for provider
- **WHEN** `app.tts.cost-per-million-chars` has no entry for the resolved key
- **THEN** TTS estimated cost SHALL be null (character count is still tracked)

### Requirement: Model pricing configuration
Model pricing SHALL be configured in `application.yaml` as optional properties on each model definition under `app.llm.models`. The properties `input-cost-per-mtok` and `output-cost-per-mtok` represent USD per million tokens. TTS pricing SHALL be configured under `app.tts.cost-per-million-chars` as a map of provider name (or model name for Inworld) to cost rate.

#### Scenario: TTS pricing configured per provider
- **WHEN** `application.yaml` includes `app.tts.cost-per-million-chars.openai: 15.00`, `app.tts.cost-per-million-chars.elevenlabs: 30.00`, `app.tts.cost-per-million-chars.inworld-tts-1.5-max: 10.00`, and `app.tts.cost-per-million-chars.inworld-tts-1.5-mini: 5.00`
- **THEN** the cost estimator uses the appropriate rate for each provider/model

#### Scenario: Model with pricing configured
- **WHEN** a model definition includes `input-cost-per-mtok: 3.00` and `output-cost-per-mtok: 15.00`
- **THEN** the cost estimator uses these values for that model's cost calculations

#### Scenario: Model without pricing configured
- **WHEN** a model definition omits pricing properties
- **THEN** the cost estimator returns null for estimated cost but token counts are still tracked

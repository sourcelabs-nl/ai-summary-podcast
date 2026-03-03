## MODIFIED Requirements

### Requirement: Inworld API sends applyTextNormalization
The `InworldApiClient.synthesizeSpeech()` SHALL always include `applyTextNormalization: "ON"` in the request body. This enables Inworld's built-in text normalization as a safety net for numbers, dates, and currencies that the LLM may not have expanded to spoken form.

#### Scenario: Text normalization enabled in API request
- **WHEN** a TTS request is sent to the Inworld API
- **THEN** the request body includes `"applyTextNormalization": "ON"`

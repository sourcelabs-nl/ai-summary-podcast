## Why

Episodes track which LLM models were used (`filterModel`, `composeModel`) but not which TTS model generated the audio. This makes it difficult to correlate audio quality or costs with specific model versions, and provides incomplete traceability when TTS models change over time.

## What Changes

- Add a `ttsModel` field to the Episode entity and database schema
- TTS providers return the model identifier they used in `TtsResult`
- `TtsPipeline` persists the TTS model on the episode after audio generation
- The episode API response includes the `ttsModel` field

## Capabilities

### New Capabilities

_(none)_

### Modified Capabilities

- `tts-generation`: TTS result now includes the model identifier; pipeline persists it on the episode
- `cost-tracking`: Episode response exposes `ttsModel` alongside existing cost fields

## Impact

- **Database**: New migration adding `tts_model` column to `episodes` table
- **API**: `EpisodeResponse` gains a new nullable `ttsModel` field (non-breaking, additive)
- **TTS providers**: `TtsResult` gains a `model` field; all providers updated to populate it
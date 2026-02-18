## Why

The ElevenLabs Text-to-Dialogue API has a 5000 character limit per request. Dialogue scripts for longer podcasts easily exceed this (e.g. 9076 chars), causing HTTP 400 `text_too_long` errors and failed episode generation.

## What Changes

- Update `ElevenLabsDialogueTtsProvider` to split dialogue turns into batches that stay under the 5000 character API limit
- Each batch produces a separate audio chunk via the Text-to-Dialogue API
- Multiple chunks are concatenated via FFmpeg (using existing `requiresConcatenation` flag)
- Single turns that exceed the limit are handled gracefully

## Capabilities

### New Capabilities

_(none)_

### Modified Capabilities

- `elevenlabs-tts`: Update the ElevenLabs Text-to-Dialogue provider requirement to handle scripts exceeding the 5000 character API limit by batching turns into multiple API calls.

## Impact

- **Code**: `ElevenLabsDialogueTtsProvider` gains batching logic; `ElevenLabsDialogueTtsProviderTest` gains chunking tests
- **Existing behavior**: Short dialogues (under 5000 chars) continue to work exactly as before with a single API call
- **Audio quality**: Batch boundaries occur between speaker turns, so there are no mid-sentence cuts
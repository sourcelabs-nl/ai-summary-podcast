## Context

Episodes already track LLM model provenance (`filterModel`, `composeModel`) but not the TTS model used for audio generation. The TTS model is currently determined per-provider: hardcoded to `"eleven_flash_v2_5"` in `ElevenLabsApiClient`, and Spring AI's default (`"tts-1"`) for OpenAI. There is no per-episode record of which model produced the audio.

## Goals / Non-Goals

**Goals:**
- Store the TTS model identifier on each episode after audio generation
- Surface the TTS model in the episode API response
- Propagate the model identifier from TTS providers through `TtsResult` to `TtsPipeline`

**Non-Goals:**
- Making the TTS model configurable per-podcast (currently hardcoded per provider — that's a separate change)
- Backfilling `ttsModel` for existing episodes

## Decisions

### Add `model` field to `TtsResult`
Each TTS provider already knows which model it uses. Adding a `model: String` field to `TtsResult` is the natural place to capture this — it flows through the existing pipeline without changing the provider interface signature.

**Alternative considered:** Reading the model from podcast config — rejected because the model is currently not configurable at podcast level, and the provider is the source of truth for what model was actually used.

### Store as nullable column
The `tts_model` column is nullable to maintain compatibility with existing episodes that were generated before this change. This follows the same pattern used for `filter_model`, `compose_model`, and the cost tracking columns.

## Risks / Trade-offs

- [Hardcoded model strings] → If the hardcoded model identifiers in providers change, the stored values become stale for new episodes. Mitigated by the fact that the value reflects what was actually used at generation time, which is the desired behavior.
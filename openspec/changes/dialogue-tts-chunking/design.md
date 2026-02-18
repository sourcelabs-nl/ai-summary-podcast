## Context

`ElevenLabsDialogueTtsProvider` currently sends all dialogue turns in a single `POST /v1/text-to-dialogue` API call. The ElevenLabs API has a 5000 character limit on the total text in a single request. Longer dialogues (typical for podcast scripts of 1000+ words) fail with HTTP 400.

The existing `TtsPipeline` already supports concatenation of multiple audio chunks via FFmpeg, controlled by the `requiresConcatenation` flag on `TtsResult`.

## Goals / Non-Goals

**Goals:**
- Handle dialogue scripts of any length by batching turns into groups under 5000 characters
- Reuse existing FFmpeg concatenation in TtsPipeline for multi-batch audio
- Maintain single-call behavior for short dialogues (no regression)

**Non-Goals:**
- Handling single turns that exceed 5000 characters (unlikely in practice — a single turn would be ~800 words)
- Streaming or parallel API calls for batches

## Decisions

### 1. Batch at the turn level in `ElevenLabsDialogueTtsProvider`

**Decision**: After parsing dialogue turns into `DialogueInput` list, group them into batches where each batch's total text length stays under 5000 characters. Never split a single turn across batches.

**Why**: Turn-level batching preserves natural conversation flow at batch boundaries. The existing `DialogueScriptParser` already gives us clean turn boundaries. Splitting mid-turn would produce unnatural audio cuts.

**Alternatives considered**:
- Reduce script length via `DialogueComposer` prompt: Fragile, limits content quality, doesn't solve the root cause
- Use ElevenLabs Studio API for long-form: Different API, unnecessary complexity

### 2. Set `requiresConcatenation = true` when multiple batches

**Decision**: When batching produces multiple audio chunks, set `requiresConcatenation = true` so `TtsPipeline` invokes FFmpeg to concatenate them.

**Why**: Reuses the existing concatenation infrastructure. Single-batch dialogues still get `requiresConcatenation = false` (no FFmpeg overhead).

## Risks / Trade-offs

- **Audio seam at batch boundaries** → Mitigation: Batches break between speaker turns, which are natural pause points. Audio quality impact should be minimal.
- **More API calls for long dialogues** → Acceptable trade-off. A 10,000-char script needs 2 calls instead of 1. Rate limiting is handled by existing error handling.

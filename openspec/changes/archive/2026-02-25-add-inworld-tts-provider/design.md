## Context

The TTS pipeline currently supports OpenAI and ElevenLabs. Script composition (LLM prompts) is TTS-provider-agnostic — the same script goes to any provider. Inworld AI TTS supports rich expressiveness features (emotion tags, emphasis markup, non-verbal cues) that require the LLM to be explicitly instructed to use them. Without provider-aware prompting, Inworld's expressive capabilities go unused.

Current flow:
```
Composer → generic script → TTS Provider → audio
```

Target flow:
```
Composer + provider guidelines → optimized script → TTS Provider → audio
```

Inworld AI uses JWT authentication (key + secret), has a 2000 character limit per request, returns base64-encoded audio, and supports both Max ($10/M chars) and Mini ($5/M chars) models.

## Goals / Non-Goals

**Goals:**
- Add Inworld AI as a TTS provider with full feature parity (monologue + dialogue styles)
- Make script composition TTS-provider-aware via `scriptGuidelines()` on `TtsProvider`
- Make `TextChunker` max chunk size configurable per provider
- Add Inworld voice discovery
- Add model-configurable pricing for Inworld (Max/Mini)

**Non-Goals:**
- Changing the system prompt / user prompt split (all composers continue using user prompts only)
- Normalizing emotion tags across providers (each provider uses its own syntax)
- Voice cloning support for Inworld
- Streaming TTS (Inworld supports it but it's out of scope)

## Decisions

### 1. Script guidelines on TtsProvider interface

**Decision:** Add `scriptGuidelines(style: PodcastStyle): String` directly to the `TtsProvider` interface.

**Why:** Each provider knows its own capabilities. Keeping guidelines co-located with the generation logic avoids a separate registry. The `ElevenLabsDialogueTtsProvider` can return the same guidelines as the single-speaker provider since they share expression capabilities.

**Alternative considered:** Separate `TtsScriptProfile` interface registered per `TtsProviderType`. Rejected because it adds indirection without clear benefit — the provider already exists as a Spring bean.

### 2. Appended guidelines block (not full prompt templates)

**Decision:** Composers inject `scriptGuidelines()` output as an additional block in the existing prompt. No structural prompt changes.

**Why:** Existing prompts are well-tested and work across styles. A guidelines block is additive and easy to extend. Full prompt templates per provider would duplicate shared logic and increase maintenance burden.

### 3. Style-aware guidelines

**Decision:** `scriptGuidelines(style: PodcastStyle)` receives the style so providers can adapt (e.g., Inworld encourages filler words for CASUAL but suppresses them for EXECUTIVE_SUMMARY).

**Why:** Expressiveness features that sound natural in casual content sound out of place in formal briefings. Style awareness lets each provider tune its guidelines per context.

### 4. Dialogue via per-turn generation

**Decision:** For dialogue/interview styles with Inworld, reuse `DialogueScriptParser` to split the XML-tagged script into turns, generate each turn with the appropriate voice ID, and concatenate audio chunks.

**Why:** This is the same approach OpenAI uses for dialogue. Inworld's API is single-voice per request (one `voiceId`). The existing infrastructure handles everything — parsing, per-turn generation, and concatenation.

### 5. Provider declares maxChunkSize

**Decision:** Add `val maxChunkSize: Int` to `TtsProvider`. `TextChunker.chunk()` accepts it as a parameter. OpenAI: 4096, ElevenLabs: 5000, Inworld: 2000.

**Why:** Provider-specific limits are inherent to each API. Making `TextChunker` parameterized avoids the need for each provider to re-implement sentence-boundary chunking.

**Alternative considered:** Keep chunking internal to each provider. Rejected to avoid duplicating the sentence-boundary splitting logic.

### 6. Inworld JWT authentication

**Decision:** `InworldApiClient` constructs JWT tokens from `INWORLD_AI_JWT_KEY` and `INWORLD_AI_JWT_SECRET` env vars. Per-user keys via `UserProviderConfigService` with `ApiKeyCategory.TTS` and provider name `"inworld"`.

**Why:** Follows the same pattern as ElevenLabs and OpenAI key management. The env vars serve as global fallback, with per-user overrides possible.

### 7. Model-configurable pricing

**Decision:** Support both `inworld-tts-1.5-max` ($10/M) and `inworld-tts-1.5-mini` ($5/M) via `ttsSettings["model"]`. Default to `inworld-tts-1.5-max`. Configure separate pricing entries in `application.yaml` per model name.

**Why:** Users may want the cheaper Mini model for less expressive content. The `ttsSettings` map already supports arbitrary provider-specific configuration.

### 8. Pipeline threading: resolving guidelines

**Decision:** `LlmPipeline` resolves the `TtsProvider` via `TtsProviderFactory` before composition and passes `scriptGuidelines()` output to the composer. This means `LlmPipeline` gains a dependency on `TtsProviderFactory`.

**Why:** The pipeline already knows the podcast's TTS provider type. Resolving guidelines at this level keeps composers unaware of the factory — they just receive a string parameter.

## Risks / Trade-offs

**[Risk] Inworld API rate limits unknown** → Start with sequential per-chunk requests. Add rate limiting if 429 errors occur in practice.

**[Risk] Inworld 2000 char limit fragments scripts more** → More chunks means more API calls and more concatenation seams. Mitigated by sentence-boundary chunking which preserves natural pauses.

**[Risk] LLM may not consistently use emotion tags** → The guidelines are advisory. If the LLM ignores them, the script still works — just without expressiveness. This is graceful degradation, not failure.

**[Trade-off] TtsProvider interface change breaks existing implementations** → All three existing providers must implement `scriptGuidelines()` and `maxChunkSize`. This is a compile-time breakage, easy to catch and fix in the same PR.

**[Trade-off] LlmPipeline now couples to TTS provider awareness** → Acceptable because the pipeline is already the orchestration layer that knows about both LLM and TTS. The coupling is at the string level (guidelines text), not at the API level.
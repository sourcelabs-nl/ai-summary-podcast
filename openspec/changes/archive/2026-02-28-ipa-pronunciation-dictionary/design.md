## Context

Inworld TTS supports inline IPA phoneme notation (`/phoneme/`) for precise pronunciation of proper nouns. The LLM is instructed via `scriptGuidelines()` to use this notation, but has no source of truth for correct pronunciations. Names like "Jarno" and "Stephan" (Dutch) get anglicized, and domain terms like "LLaMA" or "Anthropic" are often mispronounced.

Currently, `scriptGuidelines(style: PodcastStyle): String` is called in `LlmPipeline` and passed to composers. The podcast entity already uses `Map<String, String>?` fields (e.g., `ttsSettings`, `ttsVoices`, `speakerNames`) stored as JSON TEXT in SQLite with automatic converters.

## Goals / Non-Goals

**Goals:**
- Allow users to define per-podcast pronunciation mappings (term → IPA phoneme)
- Inject these mappings into the LLM prompt so generated scripts use correct IPA notation on first occurrence of each term
- Follow existing patterns for Map fields on the Podcast entity

**Non-Goals:**
- Auto-detecting mispronounced words (user must manually configure)
- Pronunciation support for non-Inworld TTS providers (IPA phonemes are Inworld-specific)
- A pronunciation management UI (API-only for now)

## Decisions

### Decision 1: Add `pronunciations` parameter to `scriptGuidelines()`

**Decision:** Extend the `TtsProvider.scriptGuidelines()` signature to `scriptGuidelines(style: PodcastStyle, pronunciations: Map<String, String>)` with a default empty map.

**Rationale:** The pronunciations are LLM prompt context, not TTS generation config. They belong in the script guidelines phase where the LLM is instructed how to write the script. Adding a default `= emptyMap()` avoids breaking existing callers and non-Inworld providers can simply ignore the parameter.

**Alternative considered:** Passing the entire `Podcast` object to `scriptGuidelines()` — rejected because it couples the TTS interface to the domain entity and exposes far more than needed.

### Decision 2: Only Inworld provider uses pronunciations

**Decision:** Only `InworldTtsProvider` will incorporate the pronunciations map into its guidelines output. Other providers return their existing guidelines unchanged since they don't support IPA phoneme notation.

**Rationale:** IPA `/phoneme/` syntax is Inworld-specific markup. Injecting it into OpenAI or ElevenLabs guidelines would produce garbled TTS output.

### Decision 3: Pronunciation injection format

**Decision:** Append a "Pronunciation Guide" section to the Inworld guidelines when the map is non-empty. Format each entry as `- Term: /phoneme/` and instruct the LLM to use the IPA notation on first occurrence of each term in the script.

**Rationale:** "First occurrence only" keeps the script readable and avoids cluttering every mention with phoneme notation. The format is clear for the LLM to follow.

### Decision 4: Store as nullable JSON TEXT column

**Decision:** Add `pronunciations TEXT` column via Flyway migration. Use existing `Map<String, String>?` pattern with `null` default.

**Rationale:** Identical to how `ttsSettings`, `ttsVoices`, `speakerNames`, and `sponsor` are stored. The existing `StringToMapConverter`/`MapToStringConverter` in `SqliteDialectConfig.kt` handles serialization automatically.

## Risks / Trade-offs

**[Risk] LLM may not consistently apply IPA on first occurrence** → Mitigation: Clear, explicit instructions in the guidelines. The post-processor does not strip IPA notation so correctly placed phonemes will pass through.

**[Trade-off] Interface signature change affects all TtsProvider implementations** → Using a default parameter (`= emptyMap()`) means existing implementations compile without changes. Only InworldTtsProvider needs to use the parameter.

**[Trade-off] No validation of IPA notation** → Users could enter invalid IPA strings. Since these are passed as-is to the LLM prompt and then to Inworld TTS, invalid IPA will simply be spoken literally (graceful degradation).

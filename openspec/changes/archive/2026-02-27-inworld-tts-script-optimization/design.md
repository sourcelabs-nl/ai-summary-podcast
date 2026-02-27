## Context

The Inworld TTS provider (`InworldTtsProvider`) already supports style-aware script guidelines that instruct the LLM to use Inworld-specific markup (non-verbal tags, emphasis, IPA phonemes). However, Inworld's official best practices documentation recommends additional formatting rules — text normalization, avoiding markdown, using contractions — that the current guidelines don't cover. Additionally, there is no post-processing step to catch LLM formatting mistakes before sending text to the Inworld API, and the `applyTextNormalization` API parameter is not being sent.

## Goals / Non-Goals

**Goals:**
- Improve Inworld TTS audio quality by aligning script guidelines with Inworld's documented best practices
- Add a post-processing step that sanitizes LLM output for Inworld-specific pitfalls
- Pass `applyTextNormalization` to the Inworld API as a safety net
- Set a sensible default temperature for non-realtime podcast generation

**Non-Goals:**
- Changing script guidelines for other TTS providers (OpenAI, ElevenLabs)
- Adding a pronunciation dictionary (separate change)
- Modifying the LLM composer prompts themselves — only the injected `ttsScriptGuidelines` block changes
- Changing the chunking strategy or max chunk size

## Decisions

### 1. Post-processing as a dedicated utility class

**Decision:** Create an `InworldScriptPostProcessor` object with a single `process(script: String): String` method that applies all sanitization steps in sequence.

**Rationale:** The post-processing is Inworld-specific and doesn't belong in the generic `TextChunker` or composer code. A dedicated utility is easy to test in isolation. Using a Kotlin `object` (singleton) since it has no state or dependencies.

**Alternatives considered:**
- Adding post-processing to `TextChunker` — rejected because it's provider-specific, not generic
- Adding it to the composers — rejected because composers are provider-agnostic; they receive guidelines but shouldn't know about provider-specific cleanup

### 2. Post-processing steps and order

**Decision:** Apply the following transformations in order:
1. Convert `**word**` → `*word*` (fix double-asterisk emphasis)
2. Strip markdown headers (`# ...`, `## ...`, etc.)
3. Strip markdown bullet points (`- ...` and `* ...` at line start — but only when followed by a space, to avoid stripping `*emphasis*`)
4. Strip markdown links `[text](url)` → keep `text`
5. Strip emojis
6. Whitelist non-verbal tags — keep only `[sigh]`, `[laugh]`, `[breathe]`, `[cough]`, `[clear_throat]`, `[yawn]`; strip any other `[word]` tags

**Rationale:** Order matters — double-asterisk conversion must happen before bullet stripping to avoid conflicts. The whitelist approach for non-verbal tags prevents the LLM from mixing in ElevenLabs-style emotion cues (`[cheerfully]`) which Inworld doesn't support.

### 3. Where to invoke post-processing

**Decision:** Call `InworldScriptPostProcessor.process()` inside `InworldTtsProvider` before chunking, for both monologue and dialogue paths.

For monologue: post-process the full script before `TextChunker.chunk()`.
For dialogue: post-process each `DialogueTurn.text` before chunking turns.

**Rationale:** Post-processing at the provider level keeps it close to the TTS-specific concerns. Processing before chunking ensures chunks receive clean text.

### 4. `applyTextNormalization` as a belt-and-suspenders approach

**Decision:** Always send `applyTextNormalization: true` in the API request body. Also instruct the LLM to write numbers/dates in spoken form via script guidelines.

**Rationale:** The LLM guidelines handle the common case, and the API parameter catches edge cases the LLM misses. There's no downside — if the LLM already wrote "twenty twenty-six", the Inworld normalizer has nothing to expand.

### 5. Default temperature of 0.8

**Decision:** When no explicit `temperature` is configured in `ttsSettings`, default to `0.8` instead of omitting it (which lets the Inworld API use its default of `1.1`).

**Rationale:** Inworld docs recommend 0.6–1.0 for non-realtime applications. The API default of 1.1 is optimized for real-time conversational use. `0.8` provides a good balance of expressiveness and consistency for pre-generated podcast audio.

## Risks / Trade-offs

- **LLM ignoring guidelines** → Mitigated by post-processing as a safety net. Even if the LLM outputs markdown or double asterisks, the post-processor cleans it up.
- **Over-aggressive tag stripping** → The whitelist approach means any future Inworld non-verbal tags won't work until added to the whitelist. Mitigated by keeping the whitelist as a constant that's easy to update.
- **Emoji regex edge cases** → Unicode emoji detection can be imperfect. Use a well-tested regex pattern covering common emoji ranges.
- **`applyTextNormalization` expanding things incorrectly** → Low risk for podcast content. If it becomes an issue for specific domains, it can be toggled off via `ttsSettings`.

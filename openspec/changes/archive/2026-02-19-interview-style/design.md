## Context

The system currently supports five podcast styles: four monologue styles (`news-briefing`, `casual`, `deep-dive`, `executive-summary`) handled by `BriefingComposer`, and one multi-speaker style (`dialogue`) handled by `DialogueComposer`. The `dialogue` style creates a symmetrical conversation where speakers contribute equally. The TTS pipeline already supports multi-voice output via `ElevenLabsDialogueTtsProvider` with role-based voice mapping through XML speaker tags.

Speaker roles are currently defined implicitly by the keys in the `ttsVoices` map (e.g., `{"host": "id1", "cohost": "id2"}`), but speakers have no display names — the LLM uses role names directly in conversation.

## Goals / Non-Goals

**Goals:**
- Add an `interview` style with an asymmetric interviewer/expert dynamic
- Allow speakers to have display names separate from their role keys
- Keep the TTS pipeline unchanged — reuse existing role-based voice mapping

**Non-Goals:**
- Changing how existing styles work (beyond adding name support to `dialogue`)
- Supporting more than two speakers for interview style
- Externalizing prompt templates to files — keeping inline for now

## Decisions

### 1. New `InterviewComposer` class

**Decision**: Create a dedicated `InterviewComposer` rather than extending `DialogueComposer` with conditional logic.

**Why**: The interview prompt is fundamentally different from dialogue. The asymmetric dynamic (interviewer ~20% of words, expert ~80%) requires distinct prompt engineering — different speaker instructions, different conversation flow patterns, different expectations for turn length. Mixing both into one class would create complex branching that's harder to maintain and test.

**Alternative considered**: Adding an `if (style == "interview")` branch inside `DialogueComposer`. Rejected because the prompts diverge significantly, and a clean separation follows the existing pattern (`BriefingComposer` vs `DialogueComposer`).

### 2. Fixed role keys for interview style

**Decision**: The `interview` style enforces exactly two roles: `interviewer` and `expert`. These are the required keys in `ttsVoices` when `style == "interview"`.

**Why**: Fixed roles let the prompt be precise about each speaker's behavior. The interviewer asks questions, bridges topics, reacts — the expert delivers substance. With freeform role names, the prompt would have to infer behavior from arbitrary strings.

**Validation**: `PodcastController.validateTtsConfig()` enforces that `interview` style requires ElevenLabs as TTS provider and exactly the keys `interviewer` and `expert` in `ttsVoices`.

### 3. New `speakerNames` field on Podcast entity

**Decision**: Add `speakerNames` as a `Map<String, String>?` (nullable, TEXT column stored as JSON) to the `Podcast` entity. Keys match `ttsVoices` keys, values are display names (e.g., `{"interviewer": "Alice", "expert": "Bob"}`).

**Why**: Speaker names are a distinct concept from voice IDs. Embedding names in `customInstructions` is fragile and unstructured. A dedicated field is validated, typed, and can be used by any composer. The same JSON converter pattern used for `ttsVoices` and `llmModels` applies here.

**Scope**: Both `InterviewComposer` and `DialogueComposer` use `speakerNames` when provided. For `DialogueComposer`, names are optional — when absent, behavior is unchanged (uses role keys as before).

### 4. Pipeline routing via `when` expression

**Decision**: Extend the composer selection in `LlmPipeline` from a binary `if/else` to a `when` expression:

```
when (podcast.style) {
    "dialogue"  -> dialogueComposer.compose(...)
    "interview" -> interviewComposer.compose(...)
    else        -> briefingComposer.compose(...)
}
```

**Why**: Clean, extensible, and follows Kotlin idioms. Adding future styles is a one-line addition.

## Risks / Trade-offs

**[Prompt quality depends on LLM following asymmetric instructions]** → The interview dynamic requires the LLM to consistently produce short interviewer turns and long expert turns. Testing with the compose model (e.g., Claude Sonnet) should validate this. The prompt will include explicit guidance on word distribution and turn length expectations.

**[Speaker names add optional complexity]** → Names are nullable and optional for all styles. Existing podcasts are unaffected. The migration adds a nullable column with no default.

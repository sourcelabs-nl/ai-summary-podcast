## Approach

Follow the existing pattern for JSON map fields on Podcast (same as `ttsSettings`, `speakerNames`, `ttsVoices`). The `sponsor` field uses the same `MapReadingConverter`/`MapWritingConverter` already registered for other JSON map columns — no new converter needed.

## Key Decisions

### Sponsor field shape
- `Map<String, String>?` with keys `name` and `message`
- Example: `{"name": "source-labs", "message": "experts in agentic software development"}`
- Nullable — when null, no sponsor instructions in the prompt

### Prompt injection
- When `podcast.sponsor` is non-null, inject two lines into the prompt:
  1. After intro: `"Immediately after the introduction, include the sponsor message: \"This podcast is brought to you by {name} — {message}.\""`
  2. Sign-off: `"End with a sign-off that includes a mention of the sponsor: {name}"`
- When null, these lines are omitted entirely (no empty placeholders)

### No validation on keys
- The API accepts any map — the composers only read `name` and `message` keys. Extra keys are ignored. This keeps it simple and forward-compatible.

## Changes by Layer

### Database (Flyway V10)
- `ALTER TABLE podcasts ADD COLUMN sponsor TEXT` — nullable, stores JSON

### Entity
- `Podcast.kt`: add `val sponsor: Map<String, String>? = null`

### API (PodcastController)
- `CreatePodcastRequest`: add `sponsor: Map<String, String>? = null`
- `UpdatePodcastRequest`: add `sponsor: Map<String, String>? = null`
- `PodcastResponse`: add `sponsor: Map<String, String>?`
- Controller create/update: pass through `sponsor` field
- Controller `toResponse()`: include `sponsor`

### Composers (BriefingComposer, DialogueComposer, InterviewComposer)
- Each composer's `buildPrompt` already receives the `Podcast` object
- Replace hardcoded sponsor lines with a conditional block built from `podcast.sponsor`
- Extract into a shared helper or just inline — given it's 3 lines of Kotlin, inline in each composer is simpler

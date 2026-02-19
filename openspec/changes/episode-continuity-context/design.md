## Context

The pipeline currently composes each episode in isolation — the `BriefingComposer` and `DialogueComposer` receive only the current batch of articles and produce a script with no awareness of what was discussed in previous episodes. Each episode already stores its full `scriptText`, and the `episode_articles` join table tracks which articles went into which episode. The infrastructure to look back at previous episodes exists but is not used during composition.

## Goals / Non-Goals

**Goals:**
- Give the composer awareness of what was discussed in the most recent episode, enabling natural continuity references
- Keep the added cost minimal by using the cheap filter model for recap generation
- Apply to both monologue (BriefingComposer) and dialogue (DialogueComposer) styles

**Non-Goals:**
- Multi-episode lookback — only the single most recent episode is considered
- Configurable toggle — this is always on when a previous episode exists
- Changing the overlap detection behavior — this is additive context for the composer, not filtering

## Decisions

### 1. Dedicated EpisodeRecapGenerator component

**Decision**: Create an `EpisodeRecapGenerator` component that takes an episode's `scriptText` and produces a 2-3 sentence recap using the filter model. The recap is a plain-text summary of the key topics discussed.

**Why**: Separating recap generation into its own component keeps it testable and decoupled from the composers. The filter model is sufficient — this is a straightforward summarization of an already-written script.

**Alternative considered**: Passing the full previous script to the composer. Rejected because episode scripts can be 1500+ words, which would significantly inflate the compose prompt token count and cost. A 2-3 sentence recap is all the composer needs.

### 2. Recap stored on the Episode entity

**Decision**: Add a nullable `recap` column to the `episodes` table. The recap is generated once when an episode is created (in `BriefingGenerationScheduler`) and stored on the episode. The pipeline reads the recap from the most recent episode directly — no LLM call needed at composition time.

**Why**: The recap is deterministic per episode — generating it once and storing it avoids redundant LLM calls on subsequent pipeline runs. It also provides observability: operators can see exactly what recap was used. The recap generation happens in `BriefingGenerationScheduler` right after the episode is saved, using the `EpisodeRecapGenerator` with the filter model.

**Alternative considered**: Generating the recap on-the-fly during each pipeline run. Rejected because it would repeat the same LLM call every time the pipeline runs, wasting tokens and adding latency.

### 3. Recap passed as optional parameter to composers

**Decision**: Add an optional `previousEpisodeRecap: String?` parameter to both `BriefingComposer.compose()` and `DialogueComposer.compose()`. When non-null, the composer includes a "Previous episode" section in the prompt with the recap text and instructions for referencing it.

**Why**: Making it optional keeps the composers backward-compatible and testable without a recap. The pipeline passes `null` when there is no previous episode or when the previous episode has no recap.

### 4. Prompt instructions for continuity behavior

**Decision**: The composition prompt receives two additions when a recap is present:
1. A "Previous episode context" block containing the recap text
2. Instructions: "When today's topics relate to the previous episode, weave in specific references (e.g., 'as we discussed last time...'). When topics are unrelated, include a brief one-liner referencing the previous episode in the introduction (e.g., 'last episode we covered X and Y, today we're looking at...')."

**Why**: This gives the LLM clear guidance on the two modes — detailed back-references for overlapping topics, brief mentions for unrelated topics. The instruction explicitly asks for brevity when there's no overlap to avoid padding.

### 5. Pipeline reads recap from most recent episode

**Decision**: In `LlmPipeline.run()`, before composition:
1. Query the most recent episode for the podcast (any terminal status: GENERATED or PENDING_REVIEW) via `EpisodeRepository`
2. Read its `recap` field (may be null for old episodes created before this feature)
3. Pass the recap to the composer

**Why**: Simple read — no LLM call in the pipeline. The recap was already generated and stored when the episode was created.

### 6. EpisodeRepository query for most recent episode

**Decision**: Add a `findMostRecentByPodcastId(podcastId)` method to `EpisodeRepository` that returns the single most recent episode, ordered by `generated_at DESC`, limited to 1. No status filter — any episode counts as context.

**Why**: Simple and efficient. Only one query needed per pipeline run. Returns null when no previous episode exists.

### 7. Recap generation timing in BriefingGenerationScheduler

**Decision**: After saving an episode (both in the review and non-review paths), generate the recap via `EpisodeRecapGenerator` and update the episode with the recap text. The recap LLM call's token usage is added to the episode's token totals.

**Why**: This is a fire-and-forget step — the recap is for the *next* episode's benefit, not the current one. Generating it immediately after creation ensures it's ready when needed. If the recap generation fails, the episode is still valid — the recap field stays null and the next pipeline run simply has no continuity context.

## Risks / Trade-offs

**[Additional LLM call per episode creation]** → Uses the cheap filter model. The input is a single episode script (~1500 words), output is 2-3 sentences. Cost is negligible compared to the composition call itself. Skipped entirely for the very first episode.

**[Recap quality depends on filter model]** → The filter model is less capable than the compose model. However, summarizing an existing script into 2-3 sentences is a simple task well within cheap model capabilities.

**[Stale references if episodes are far apart]** → If the last episode was weeks ago, referencing it may feel odd. Mitigated by the prompt instructions which let the LLM judge how to phrase the reference — the LLM sees the recap content and can decide if a "last time" reference makes sense contextually.

**[Recap generation failure]** → If the LLM call fails, the episode is saved without a recap. The next pipeline run simply has no continuity context — graceful degradation, no pipeline breakage.

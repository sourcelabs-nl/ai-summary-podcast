## 1. Data Model

- [x] 1.1 Add `findMostRecentByPodcastIdAndStatus(podcastId, status)` query method to `EpisodeRepository` returning the single most recent episode (ordered by `generated_at DESC`, limit 1)
- [x] 1.2 Add `recap` nullable column to `episodes` table via Flyway migration (V26)
- [x] 1.3 Add `recap: String?` field to `Episode` entity
- [x] 1.4 Replace `findMostRecentByPodcastIdAndStatus` with `findMostRecentByPodcastId(podcastId)` (no status filter needed — any episode provides context)

## 2. Episode Recap Generator

- [x] 2.1 Create `EpisodeRecapGenerator` component with method `generate(scriptText, podcast, filterModelDef): RecapResult` that calls the filter model LLM to produce a 2-3 sentence recap
- [x] 2.2 Write the recap prompt: instruct the LLM to summarize the main topics of the episode script concisely, no meta-commentary, plain text output
- [x] 2.3 Create `RecapResult` data class containing `recap: String` and `usage: TokenUsage`
- [x] 2.4 Add unit tests for `EpisodeRecapGenerator`: recap produced from script, token usage tracked, empty/short script handling

## 3. Recap Persistence in Scheduler

- [x] 3.1 Inject `EpisodeRecapGenerator` and `ModelResolver` into `BriefingGenerationScheduler`
- [x] 3.2 After saving an episode (both review and non-review paths), call `EpisodeRecapGenerator.generate()` and update the episode with the recap text and token usage. Wrap in try-catch so failure does not affect the episode.
- [x] 3.3 Add unit tests for scheduler recap generation: recap stored on episode, recap failure does not block episode, token usage added to episode totals

## 4. Composer Prompt Updates

- [x] 4.1 Add optional `previousEpisodeRecap: String?` parameter to `BriefingComposer.compose()` methods and extend `buildPrompt()` to include "Previous episode context" section and continuity instructions when recap is non-null
- [x] 4.2 Add optional `previousEpisodeRecap: String?` parameter to `DialogueComposer.compose()` methods and extend `buildPrompt()` to include "Previous episode context" section and continuity instructions when recap is non-null
- [x] 4.3 Add unit tests for `BriefingComposer.buildPrompt()`: prompt includes recap section when provided, prompt excludes recap section when null
- [x] 4.4 Add unit tests for `DialogueComposer.buildPrompt()`: prompt includes recap section when provided, prompt excludes recap section when null

## 5. Pipeline Integration

- [x] 5.1 Add recap reading to `LlmPipeline.run()`: before composition, fetch most recent episode for the podcast, read its recap field, pass to composer
- [x] 5.2 Add INFO logging for recap context (recap found / no previous episode / recap null)
- [x] 5.3 Add unit tests for pipeline with recap: recap read from previous episode and passed to composer, no recap when no previous episode, no recap when previous episode has null recap

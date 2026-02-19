## Why

Manual episode generation via `PodcastController.generate()` does not save episode-article links, causing two cascading failures: (1) discarding manually-generated episodes fails to reset articles for reprocessing, permanently losing them, and (2) subsequent episodes receive very few articles, causing the LLM to pad scripts with its own training knowledge — producing content that overlaps significantly with previous episodes.

## What Changes

- Fix `PodcastController.generate()` to save episode-article links and generate episode recaps, matching the scheduler's behavior (delegates to shared logic instead of duplicating it)
- Fix `EpisodeController.discard()` to also reset articles directly via `processedArticleIds` from the pipeline result, as a fallback when episode-article links are missing (handles existing episodes without links)
- Add explicit grounding instructions to all three composer prompts (BriefingComposer, DialogueComposer, InterviewComposer) to prevent the LLM from introducing content not present in the provided articles
- Add a development guideline to `CLAUDE.md` requiring controllers to delegate to service/domain logic rather than reimplementing pipeline behavior

## Capabilities

### New Capabilities

_None_

### Modified Capabilities

- `episode-article-tracking`: Manual generate endpoint must save article links and generate recaps, matching scheduler behavior
- `llm-processing`: All composer prompts must include grounding instructions constraining output to provided article content
- `episode-review`: Discard must handle episodes without article links gracefully (reset articles via pipeline result stored on episode)

## Impact

- `PodcastController.kt` — refactor generate endpoint to delegate to shared generation logic
- `BriefingComposer.kt`, `DialogueComposer.kt`, `InterviewComposer.kt` — add grounding constraint to prompts
- `EpisodeController.kt` — improve discard fallback for missing article links
- `CLAUDE.md` — add architectural guideline
- Tests for all modified components

## Context

The LLM pipeline (`LlmPipeline.run()`) currently runs as a single unit: score articles, compose script, mark articles as processed, return PipelineResult. The pipeline is called by `PodcastService.generateBriefing()` which then creates an Episode entity. There's no way to run parts of the pipeline without the side effects.

## Goals / Non-Goals

**Goals:**
- Expose upcoming article pool for a podcast (relevant, unprocessed)
- Provide dry-run script preview (LLM compose without persisting episode or marking articles)
- Show upcoming content count in the episodes list for quick visibility

**Non-Goals:**
- Editing/curating the article pool before generation
- Caching preview scripts (each preview is a fresh LLM call)
- Showing unscored articles (only scored + relevant)

## Decisions

**Dry-run via new pipeline method**: Add a `preview()` method to LlmPipeline that reuses the scoring and composition steps but skips article processing (does not call `article.copy(isProcessed = true)` or save). This avoids duplicating composition logic. Scoring IS persisted (it's cheap and useful regardless).

**Preview response is ephemeral**: The `POST /preview` endpoint returns the script text and article list directly. Nothing is saved to the episodes table. If the user wants to generate, they click "Generate Episode" which runs the full pipeline (fresh LLM call, may produce slightly different script).

**Upcoming articles endpoint**: `GET /upcoming-articles` reuses `ArticleRepository.findRelevantUnprocessedBySourceIds()` — the same query the pipeline uses for article selection. This ensures the preview accurately reflects what would go into the next episode.

**Frontend preview page**: `/podcasts/{podcastId}/upcoming` shows articles grouped by source (reuse ArticlesTab pattern). Preview and Generate are buttons in the header. Preview result shows on a sub-page `/podcasts/{podcastId}/upcoming/preview` using ScriptContent component. Generate navigates to the new episode's detail page.

**Upcoming row in Episodes tab**: A styled row at the top of the episodes table (before episode rows) showing "Next Episode · N articles ready" with a chevron linking to the upcoming page. Only shown when there are upcoming articles.

## Risks / Trade-offs

- [Cost] Each preview costs LLM tokens for composition → Acceptable; user explicitly requests it
- [Staleness] Time between preview and generate could mean different articles → Fine; both actions are on-demand
- [Scoring side effect] Preview triggers scoring of unscored articles → By design; scoring is cheap and the scores are useful data regardless of whether an episode is generated

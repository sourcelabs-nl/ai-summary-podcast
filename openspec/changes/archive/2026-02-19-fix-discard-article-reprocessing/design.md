## Context

When a user discards a `PENDING_REVIEW` episode, the discard endpoint sets the episode status to `DISCARDED` but does not touch the linked articles. Those articles were marked `isProcessed = true` during pipeline execution (`LlmPipeline.kt:121`), so they are permanently excluded from future episode generation by the query in `ArticleRepository.findRelevantUnprocessedBySourceIds`.

The `episode_articles` join table already tracks which articles belong to which episode, so the link information needed to identify affected articles is available.

## Goals / Non-Goals

**Goals:**
- When an episode is discarded, reset `isProcessed` on its linked articles so they re-enter the pipeline for the next generation cycle.

**Non-Goals:**
- Changing the discard API contract (request/response shape stays the same).
- Handling article reset for other status transitions (e.g., `FAILED` episodes — those are retried via approve, not discarded).
- Deleting the `episode_articles` links on discard — keeping them preserves the audit trail.

## Decisions

### Reset articles inside the discard endpoint

**Decision**: Add article reset logic directly in `EpisodeController.discard()`, using `EpisodeArticleRepository.findByEpisodeId()` to look up linked article IDs, then setting `isProcessed = false` on each.

**Alternatives considered**:
- *Event-driven reset via ApplicationEvent*: Overkill for a single synchronous operation within one transaction.
- *Bulk SQL update (`UPDATE articles SET is_processed = 0 WHERE id IN (...)`)* via a custom repository query: Cleaner for large article counts, but episode-article counts are small (typically 5-20). Individual saves are fine and consistent with the existing codebase style.

**Rationale**: Keeps the change minimal and co-located with the status update. The existing codebase uses `repository.save()` for individual updates throughout — this follows the same pattern.

## Risks / Trade-offs

- **[Risk] Re-processed articles produce similar content** → Acceptable. The user explicitly chose to discard the episode, implying they want those articles reconsidered (possibly with different composition). The LLM will compose a new script regardless.
- **[Risk] Concurrent pipeline run picks up articles mid-reset** → Low risk. The scheduler runs on a 60-second fixed delay and checks for pending/approved episodes before generating. The discard + reset completes in milliseconds.

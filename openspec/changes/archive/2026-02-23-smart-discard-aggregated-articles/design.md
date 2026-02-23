## Context

When an episode is discarded, `EpisodeService.discardAndResetArticles()` resets all linked articles by setting `isProcessed = false`. This works well for non-aggregated articles (1:1 post-to-article), but for aggregated articles (many posts combined into one article via `post_articles`), the stale aggregated article gets reused as-is. If new posts arrived since, the next pipeline run creates a second aggregated article alongside the old one, leading to overlapping content.

The aggregation of posts into articles happens in `SourceAggregator.aggregateAndPersist()`, called from `LlmPipeline.run()` Step 1. Posts are eligible for aggregation when they have no `post_articles` link (i.e., they are "unlinked").

## Goals / Non-Goals

**Goals:**
- Aggregated articles are properly cleaned up on episode discard so posts can be re-aggregated fresh on the next pipeline run
- Non-aggregated articles continue to work as today (reset `isProcessed`)

**Non-Goals:**
- Changing how aggregation itself works in `SourceAggregator`
- Changing the pipeline flow or scheduling

## Decisions

### Detecting aggregated articles

**Decision**: Check for multiple `post_articles` entries for a given article ID. If an article has 2+ linked posts, it's an aggregated article. If it has 0 or 1, it's a non-aggregated article.

**Rationale**: This is the simplest detection — it uses existing data without needing to look up the source or its `aggregate` flag. An article with multiple posts was produced by aggregation, regardless of the source configuration.

**Alternative considered**: Look up the source and check `shouldAggregate()`. Rejected because it couples `EpisodeService` to source configuration logic and requires additional queries. The post count is a direct, reliable indicator.

### Cleanup strategy for aggregated articles

**Decision**: For aggregated articles, delete the `post_articles` links first, then delete the article itself. This makes the posts "unlinked" again, so `findUnlinkedBySourceIds()` picks them up on the next pipeline run.

**Rationale**: Deleting is cleaner than resetting — the aggregated article's content hash, summary, and score are all tied to the specific combination of posts. Re-aggregation will produce a different article anyway (potentially with new posts included).

### New repository methods

**Decision**: Add `PostArticleRepository.findByArticleId(articleId)` (already exists) and `PostArticleRepository.deleteByArticleId(articleId)` for cleanup. Use `ArticleRepository.deleteById(id)` (inherited from `CrudRepository`) for article deletion.

**Rationale**: Keep the cleanup logic in `EpisodeService` since it's part of the discard workflow. No need for a new service — this is a small addition to existing behavior.

## Risks / Trade-offs

- **[Aggregated article scores are lost]** → Accepted. The re-aggregated article will have different content (potentially including new posts), so re-scoring is necessary anyway. This costs a small number of LLM tokens.
- **[Transaction boundary]** → The method is already `@Transactional`, so deleting post-article links and the article is atomic with the episode status update.

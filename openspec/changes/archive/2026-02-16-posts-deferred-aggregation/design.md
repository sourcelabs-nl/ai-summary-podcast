## Context

The current pipeline aggregates short-form content (tweets, Nitter posts) into articles at poll time in `SourceAggregator`, discarding individual items. The `articles` table stores both raw long-form content and pre-aggregated digests. The LLM pipeline processes articles through three sequential stages: relevance scoring, summarization, and script composition.

This change introduces a `posts` table as the universal ingest target, defers aggregation to script generation time, and simplifies the LLM pipeline from three stages to two.

## Goals / Non-Goals

**Goals:**
- Preserve individual content items (posts) for reprocessability and traceability
- Defer aggregation from poll time to script generation time with configurable time windows
- Maintain full linkage between posts and articles via a join table
- Simplify the LLM pipeline by merging scoring + summarization + filtering into one call
- Keep a uniform data path: all source types write to `posts`

**Non-Goals:**
- Topic-based clustering of posts across sources (future enhancement)
- Changing the podcast feed format or episode structure
- Migrating existing articles to have post linkage (existing processed articles stay as-is)
- Changing the TTS pipeline or audio generation

## Decisions

### 1. New `posts` table as universal ingest target

All source pollers (`RssFeedFetcher`, `TwitterFetcher`, `WebsiteFetcher`) write to a `posts` table instead of `articles`. The `Post` entity mirrors the current article fields needed at ingest time: `sourceId`, `title`, `body`, `url`, `publishedAt`, `author`, `contentHash`.

**Why**: A uniform data path simplifies the polling code and ensures every content item is preserved individually, regardless of source type.

**Alternative considered**: Only use `posts` for aggregated source types, keep RSS writing directly to `articles`. Rejected because two code paths increase complexity and the 1:1 pass-through for non-aggregated sources adds negligible overhead.

### 2. `post_articles` join table for traceability

A `post_articles` table links `post_id` to `article_id`. When aggregation creates an article from N posts, N rows are inserted in the join table. For non-aggregated sources, a single row links the one post to its article.

**Why**: Enables reprocessing (delete article, re-aggregate from same posts), multi-podcast support (same post can be linked to articles in different podcasts), and auditability (which posts composed which article/episode).

**Alternative considered**: Flag on posts (`isProcessed`) — rejected because it prevents reprocessing and loses the post→article linkage. Time-window-only approach — rejected because it loses explicit traceability.

### 3. Time-windowed aggregation at script generation time

For aggregated sources, the `SourceAggregator` is invoked during script generation (in `LlmPipeline`), not during polling. It queries unprocessed posts within a configurable time window (default: value of `app.source.max-article-age-days`, matching the existing article age config). Posts outside the window are not included.

For non-aggregated sources, each unlinked post becomes a 1:1 article.

**Why**: Time windows decouple aggregation from poll cycles, allowing posts from multiple polls to combine naturally. The window default matches the existing max article age to maintain consistent behavior.

### 4. Two-stage LLM pipeline (score+summarize+filter → compose)

The current three stages (`RelevanceScorer` → `ArticleSummarizer` → `BriefingComposer`) are replaced by two:

**Stage 1 — Score, Summarize, and Filter** (per article, filter model): A single LLM call receives the full article content and returns structured JSON:
```json
{
  "relevanceScore": 7,
  "summary": "...",
  "includedPostIds": [1, 3, 7],
  "excludedPostIds": [2, 4, 5, 6]
}
```
The LLM scores relevance, filters out irrelevant posts from the summary, and summarizes the relevant content — all in one call. For non-aggregated articles (single post), `includedPostIds`/`excludedPostIds` can be omitted (the entire article is either relevant or not).

**Stage 2 — Compose** (all relevant articles, compose model): Unchanged from today — takes scored/summarized articles and composes the briefing script.

**Why**: Fewer LLM calls (1 per article instead of 2), and the model has full context to decide relevance while summarizing. The structured response gives traceability into which posts were deemed relevant.

### 5. Flyway migration V16

A single migration (`V16__add_posts_table.sql`) adds:
- `posts` table with columns: `id`, `source_id`, `title`, `body`, `url`, `published_at`, `author`, `content_hash` (unique per source), `created_at`
- `post_articles` join table with columns: `id`, `post_id` (FK), `article_id` (FK), unique constraint on `(post_id, article_id)`
- Index on `posts(source_id, content_hash)` for deduplication
- Index on `posts(source_id, created_at)` for time-windowed queries

Existing `articles` table is unchanged — no columns added or removed. Articles are now created at script generation time rather than poll time, but the schema is the same.

## Risks / Trade-offs

**[Storage increase]** → Storing individual posts plus articles (which contain aggregated post content) means some data duplication. Mitigation: posts are small (tweets are <280 chars), and the duplication is bounded by the time window. Old unprocessed posts can be cleaned up on the same schedule as old articles.

**[Aggregation timing]** → Moving aggregation to script generation time means the aggregation step now happens in the critical path of episode generation. Mitigation: aggregation is a lightweight in-memory operation (string concatenation), not an LLM call. The added latency is negligible.

**[LLM structured output reliability]** → The merged score+summarize stage relies on structured JSON output from the LLM. Mitigation: Spring AI's structured output support with JSON schema. The `includedPostIds`/`excludedPostIds` fields are optional for non-aggregated articles, reducing failure surface.

**[Backward compatibility]** → Existing articles have no corresponding posts. Mitigation: This is acceptable — existing processed articles are historical records. The `post_articles` join table simply has no entries for pre-migration articles. No data migration needed.

## Open Questions

- Should old unprocessed posts be cleaned up on the same schedule as old articles (`max-article-age-days`), or have their own retention config?
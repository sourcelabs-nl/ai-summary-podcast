## Context

The pipeline currently deduplicates posts and articles per-source using `(source_id, content_hash)`. When multiple sources within the same podcast produce identical or overlapping content (e.g., a Twitter source and a Nitter RSS mirror), duplicates pass through and get summarized into the same episode. Additionally, newly added sources fetch historical content that may already have been covered in published episodes. There is no join between episodes and articles, so there is no traceability of what content went into which episode.

## Goals / Non-Goals

**Goals:**
- Prevent duplicate content from different sources within the same podcast from being processed twice
- Ensure newly added sources only ingest forward-looking content, not historical backlog
- Track which articles contributed to which episode via a join table

**Non-Goals:**
- Semantic/fuzzy duplicate detection (same topic, different wording) — deferred to a separate change
- Cross-podcast deduplication — each podcast is independent
- Historical overlap detection against already-published episodes — deferred to a separate change
- URL-based deduplication

## Decisions

### 1. Cross-source dedup at the post level, not article level

**Decision**: Add a cross-source content hash check in `SourcePoller` at post ingestion time.

**Why**: Posts are the earliest point where content enters the system. Deduplicating here prevents duplicate articles from ever being created. The check queries all sources belonging to the same podcast: if any source already has a post with the same content hash, the new post is skipped.

**Alternative considered**: Dedup at the article level in `SourceAggregator`. Rejected because duplicate posts would still accumulate in the `posts` table, and aggregated articles compute their hash from the combined body text, making cross-source hash matching unreliable for aggregated content.

**Implementation**: `PostRepository` gains a `findByContentHashAndSourceIds(contentHash, sourceIds)` method. `SourcePoller.poll()` receives the list of sibling source IDs (all sources in the same podcast) and checks this before saving. The `SourcePollingScheduler` resolves sibling source IDs when calling `poll()`.

### 2. Forward-only ingestion via `createdAt` on sources

**Decision**: Add a `created_at` column to the `sources` table. On a source's first poll (`lastPolled = null`), skip any posts with `publishedAt` before `source.createdAt`.

**Why**: This is the simplest safeguard — no new tables or LLM calls needed. It prevents the flood of historical content when a new source is added.

**Alternative considered**: A dedicated "initial sync" flag that switches ingestion behavior. Rejected as more complex than necessary — `createdAt` + `lastPolled = null` is sufficient to detect first poll.

**Implementation**: `Source` entity gains a `createdAt: String` field. Flyway migration adds `created_at TEXT` column with default `'1970-01-01T00:00:00Z'` for existing sources (so they continue to ingest everything). The source creation endpoint (`SourceController`) sets `createdAt` to `Instant.now()`. `SourcePoller` checks `if (source.lastPolled == null && post.publishedAt != null && post.publishedAt < source.createdAt) skip`.

### 3. Episode-article join table

**Decision**: Create an `episode_articles` join table with `(episode_id, article_id)` and populate it when articles are marked as processed in `LlmPipeline`.

**Why**: This provides traceability and is a prerequisite for the future historical overlap detection feature. The natural place to record links is in `LlmPipeline.run()` right where `isProcessed = true` is set, since at that point we know exactly which articles went into the briefing.

**Alternative considered**: Recording links in `BriefingGenerationScheduler` after episode creation. Rejected because the scheduler doesn't have direct access to the article list — `LlmPipeline` returns only the script, not the articles. Passing articles through would require changing the `PipelineResult` API.

**Revised approach**: Extend `PipelineResult` to include the list of processed article IDs. Record links in `BriefingGenerationScheduler` after the episode is saved (so we have the episode ID). This keeps `LlmPipeline` focused on LLM work and puts persistence logic in the scheduler.

## Risks / Trade-offs

**[Cross-source dedup requires podcast context in SourcePoller]** → The `SourcePoller` currently doesn't know about sibling sources. The scheduler already resolves podcast context for Twitter sources, so extending this to always pass sibling source IDs is low-risk.

**[Forward-only ingestion misses posts without publishedAt]** → Posts with `publishedAt = null` cannot be age-filtered. These are allowed through, same as the existing max-article-age filter. This is acceptable because null-dated posts are uncommon in RSS feeds.

**[Migration default for existing sources]** → Setting `created_at` to epoch (`1970-01-01T00:00:00Z`) for existing sources means they are unaffected. New sources get current timestamp. No data migration risk.
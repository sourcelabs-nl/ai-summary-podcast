## Context

The `SourcePoller` fetches all entries from a feed and stores any article whose `content_hash` is not already in the database. When a new source is added, the full feed history (potentially months or years of articles) gets ingested. These old articles flow through the LLM pipeline and end up in the next briefing, producing stale content.

The existing deduplication (`content_hash` unique constraint) prevents the same article from being stored twice, but does nothing about age.

## Goals / Non-Goals

**Goals:**
- Prevent articles older than a configurable threshold from being stored during ingestion
- Make the threshold configurable with a sensible default (7 days)

**Non-Goals:**
- Filtering at the LLM pipeline level (the source poller is the right place — no point storing what we won't use)
- Handling articles with `publishedAt = null` differently (we cannot determine their age, so they pass through)

## Decisions

### Filter at ingestion, not at pipeline selection

**Decision**: Skip old articles in `SourcePoller.poll()` before saving them.

**Alternatives considered**:
- Filter in `ArticleRepository.findRelevantUnprocessedBySourceIds` — adds query complexity, still wastes storage and relevance-filtering LLM calls on articles that will be discarded
- Filter in both places — over-engineering; if old articles never enter the DB, the pipeline doesn't need to worry about them

**Rationale**: Filtering at the source is simpler and avoids wasting storage, LLM calls for relevance filtering, and summarization on articles that are too old.

### Add a `SourceProperties` group to `AppProperties`

**Decision**: Add `app.source.max-article-age-days` (default 7) via a new `SourceProperties` data class, following the existing pattern of `LlmProperties`, `EpisodesProperties`, etc.

### Use `publishedAt` for age comparison

**Decision**: Compare `article.publishedAt` against `Instant.now() - maxArticleAgeDays`. Articles with `publishedAt = null` are always included.

**Rationale**: `publishedAt` is the only timestamp we have at ingestion time. Null values get the benefit of the doubt.

### Delete existing old unprocessed articles

**Decision**: Add a `deleteOldUnprocessedArticles(cutoff)` query to `ArticleRepository` and call it from `SourcePollingScheduler` before polling. This cleans up old articles that were ingested before the age filter was in place.

**Why delete instead of marking as processed**: Marking `is_processed = true` would prevent summarization, but old articles with `is_relevant IS NULL` would still be picked up by `findUnfilteredBySourceIds` and sent to the LLM for relevance filtering — wasting API calls. Deleting removes them entirely.

**Why in the polling scheduler**: The polling scheduler already runs periodically. Running cleanup there keeps it simple — no need for a separate scheduled job.

## Risks / Trade-offs

- **[Unreliable `publishedAt`]** → Some feeds may have incorrect or missing dates. Mitigation: articles with null `publishedAt` are included; incorrect dates are an upstream data quality issue we can't solve.
- **[Losing potentially interesting old content]** → A source might have a highly relevant article from 2 weeks ago. Mitigation: the threshold is configurable; users can increase it if needed.

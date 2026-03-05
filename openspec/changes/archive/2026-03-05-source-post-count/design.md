## Context

The source list endpoint already computes article counts in a single batch query via `SourceService.getArticleCounts()`. Posts are stored in a separate `posts` table with `source_id` as the foreign key. The same batch query pattern can be reused for post counts.

## Goals / Non-Goals

**Goals:**
- Add `postCount` to source list responses using the same batch query pattern as article counts
- Display post count in the frontend sources table

**Non-Goals:**
- No post relevance tracking (posts don't have relevance scores)
- No changes to the posts table schema

## Decisions

**1. Single additional batch query for post counts**

Add a `getPostCounts()` method to `SourceService` that uses the same pattern as `getArticleCounts()` — a single SQL query with `COUNT(*) ... GROUP BY source_id`. This keeps the N+1 query prevention pattern consistent.

Alternative considered: combining article and post counts in one query with a JOIN — this would be more complex and the posts/articles tables aren't directly related by source_id in a way that makes a single query clean.

**2. Display format: "Posts / Articles (X% relevant)"**

Show post count before article count in the table cell, separated by a slash. This gives immediate visibility into the aggregation ratio.

## Risks / Trade-offs

- [Extra query per list call] → Minimal impact, it's a single indexed COUNT query. The `posts` table already has `idx_posts_source_created` index on `(source_id, created_at)`.

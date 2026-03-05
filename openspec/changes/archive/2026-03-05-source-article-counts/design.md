## Context

The source list API returns source configuration but no statistics about collected content. Article counts per source can be computed from the `articles` table which has a `source_id` foreign key and `relevance_score` column. The podcast's `relevanceThreshold` determines what counts as "relevant".

## Goals / Non-Goals

**Goals:**
- Show total article count and relevance percentage per source in the sources table
- Compute counts efficiently in a single batch query, not N+1

**Non-Goals:**
- Time-scoped counts (all-time only)
- Post counts (only aggregated articles)
- Clickable drill-down into source articles

## Decisions

### 1. Batch count query grouped by source_id

Use a single SQL query: `SELECT source_id, COUNT(*) as total, COUNT(CASE WHEN relevance_score >= :threshold THEN 1 END) as relevant FROM articles WHERE source_id IN (:sourceIds) GROUP BY source_id`.

**Rationale**: One query for all sources, avoids N+1. The threshold comes from the podcast entity.

**Alternative considered**: Separate queries per source. Rejected — unnecessary overhead.

### 2. Enrich SourceResponse via service layer

Add a service method that fetches sources + article counts and returns an enriched model. The controller calls this instead of the raw source list.

**Rationale**: Keeps business logic (threshold lookup, count computation) in the service layer per architecture guidelines.

## Risks / Trade-offs

- **[Risk] Performance on large datasets**: COUNT queries on articles table for podcasts with many sources. → **Mitigation**: Single indexed query on `source_id`, acceptable at current scale.

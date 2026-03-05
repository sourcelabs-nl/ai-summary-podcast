## 1. Backend

- [x] 1.1 Add batch count query to `ArticleRepository`: `countBySourceIds(sourceIds, threshold)` returning a list of `(sourceId, total, relevant)` tuples
- [x] 1.2 Add service method to `SourceService` that fetches sources and enriches them with article counts
- [x] 1.3 Add `articleCount` and `relevantArticleCount` fields to `SourceResponse` and update the controller list endpoint to use the enriched service method

## 2. Frontend

- [x] 2.1 Update `Source` type in `types.ts` with `articleCount` and `relevantArticleCount` fields
- [x] 2.2 Add "Articles" column to the sources table in `sources-tab.tsx` showing count and relevance percentage

## 3. Testing

- [x] 3.1 Add unit test for the service method covering: sources with articles, sources with no articles, mixed

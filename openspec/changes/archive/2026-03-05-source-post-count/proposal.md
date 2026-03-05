## Why

The source list currently shows article counts and relevance percentages, but not post counts. Posts are the raw content items fetched from sources before aggregation into articles. Showing post counts gives users visibility into how much raw content each source is producing, which is useful for tuning poll intervals and understanding aggregation ratios (e.g., 50 tweets aggregated into 3 articles).

## What Changes

- Add `postCount` field to the source list API response
- Add a batch query to count posts per source (same pattern as existing article counts)
- Display post count in the frontend sources table alongside article counts

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `source-config`: Add `postCount` to source list response and frontend display.

## Impact

- `SourceService.kt` — new batch query for post counts
- `SourceController.kt` — pass post counts to response mapping
- `SourceResponse` data class — new `postCount` field
- `frontend/src/lib/types.ts` — add `postCount` to `Source` interface
- `frontend/src/components/sources-tab.tsx` — display post count in table

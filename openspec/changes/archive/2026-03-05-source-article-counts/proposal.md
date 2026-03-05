## Why

The sources tab only shows configuration (type, interval, enabled) but gives no insight into what each source has produced. Users need to see how many articles a source has contributed and what percentage were relevant, to evaluate source quality and tune relevance thresholds.

## What Changes

- Enrich the `SourceResponse` API with article count statistics (total articles, relevant count) via a batch query
- Add an "Articles" column to the sources table showing total count and relevance percentage

## Capabilities

### New Capabilities

_(none)_

### Modified Capabilities

- `source-config`: Add article count statistics to the source list API response
- `frontend-source-management`: Add articles column to the source list table

## Impact

- **Backend**: `SourceController` response enriched with counts, new batch query in repository/service
- **Frontend**: `sources-tab.tsx` — new column, `types.ts` — updated Source interface

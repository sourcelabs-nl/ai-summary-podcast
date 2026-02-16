## Why

Sources that produce many short items per poll (Twitter/X API tweets, nitter RSS tweets, microblog feeds) create individual articles that lack context for meaningful relevance scoring and waste LLM tokens when scored/summarized individually. Aggregating these items into a single article per source per poll cycle gives the LLM a coherent batch to evaluate.

## What Changes

- Introduce a general `SourceAggregator` component that merges individual fetched items into a single consolidated article per source per poll cycle
- Add an `aggregate` column to the `sources` table (nullable boolean) to allow per-source override
- Hybrid detection: auto-detect aggregation for known source types (twitter, nitter URLs), allow explicit override via the `aggregate` flag on any source
- Integrate the aggregator into `SourcePoller` after fetching, before storage — transparent to the rest of the pipeline
- Each aggregated article contains all items as a formatted digest with timestamps and original URLs preserved

## Capabilities

### New Capabilities
- `source-aggregation`: Hybrid auto-detect + per-source override aggregation of short-form content items into a single article per source per poll cycle

### Modified Capabilities
- `source-polling`: The poller gains a post-fetch aggregation step
- `source-config`: Sources gain an optional `aggregate` field for explicit override

## Impact

- **Code**: New `SourceAggregator` component, small change to `SourcePoller.poll()`
- **Data**: New `aggregate` column on `sources` table (nullable boolean, requires DB migration). Articles from aggregated sources stored as one digest per poll instead of individual items.
- **API**: Source create/update endpoints accept optional `aggregate` field
- **LLM pipeline**: No changes — aggregated articles flow through relevance scoring, summarization, and composition as normal articles
## Why

Noisy RSS feeds (e.g. general news sites) produce many entries per poll, most of which are irrelevant. While the LLM relevance filter catches these downstream, every entry still gets ingested as a Post and costs LLM tokens to evaluate. A simple category pre-filter lets users narrow RSS sources to specific categories before ingestion, reducing noise and LLM cost.

## What Changes

- Add an optional `categoryFilter` field to the `Source` entity — a comma-separated list of category terms
- Filter RSS entries in `RssFeedFetcher` by matching entry categories against the filter terms (case-insensitive contains)
- Entries with no categories in the feed pass through unfiltered (safe default)
- Expose `categoryFilter` via the existing Source REST API (create/update)
- Add DB migration for the new column

## Capabilities

### New Capabilities
- `rss-category-filter`: Pre-ingestion filtering of RSS feed entries based on category tags, applied in the RSS fetcher before posts are created

### Modified Capabilities
- `source-config`: Add optional `categoryFilter` field to source configuration

## Impact

- `Source` entity: new nullable `categoryFilter` column
- `RssFeedFetcher`: add category filtering step
- `SourceController`/`SourceService`: pass through new field (already dynamic via Spring Data)
- DB migration: add column to `sources` table
- No breaking changes — existing sources without `categoryFilter` behave as before
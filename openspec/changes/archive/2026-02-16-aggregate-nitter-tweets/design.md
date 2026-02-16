## Context

Sources that produce many short items per poll (Twitter/X tweets, nitter RSS, microblogs) store each item as a separate article. These go through relevance scoring and summarization individually, which is wasteful for short-form content. The pipeline already handles multi-article composition well — the bottleneck is that individual tweets lack enough context for meaningful relevance scoring.

Current flow:
```
Source → Fetcher → [article, article, article, ...] → SourcePoller → DB (one row per item)
```

Desired flow:
```
Source → Fetcher → [article, article, article, ...] → SourceAggregator → [single digest article] → SourcePoller → DB
```

## Goals / Non-Goals

**Goals:**
- Aggregate short-form items into a single article per source per poll cycle
- Hybrid detection: auto-detect for known patterns (twitter type, nitter URLs) + per-source `aggregate` flag override
- Transparent to the rest of the pipeline (LLM scoring, summarization, composition)

**Non-Goals:**
- Changing how the LLM pipeline processes articles (it already handles long articles fine)
- Aggregating across multiple sources (each source aggregates independently)
- Changing the RSS fetcher itself — aggregation happens after fetching

## Decisions

### 1. Aggregation as a post-fetch step in SourcePoller

The `SourceAggregator` is called in `SourcePoller.poll()` after fetching articles but before the storage loop. This keeps fetchers unchanged and aggregation logic isolated.

**Alternative considered:** Aggregation inside each fetcher. Rejected because it duplicates logic across `RssFeedFetcher` and `TwitterFetcher`, and the fetcher's job is to extract items, not to decide how they're stored.

### 2. Hybrid detection with nullable `aggregate` column

The `Source` entity gets a nullable `aggregate: Boolean?` field:
- `null` (default) → auto-detect: aggregate if source type is `"twitter"` OR URL contains `nitter.net`
- `true` → always aggregate, regardless of type/URL
- `false` → never aggregate, even if auto-detect would say yes

**Alternative considered:** Only auto-detect (no per-source flag). Rejected because users may want to aggregate other short-form RSS feeds or disable aggregation for specific twitter sources.

### 3. Digest article format

Aggregated article structure:
- **title:** `"Posts from @{username} — {date}"` (or source domain if no username detectable)
- **body:** Items joined with `\n\n---\n\n`, each prefixed with timestamp if available
- **url:** The source URL (nitter profile or feed URL)
- **publishedAt:** Most recent `publishedAt` from the batch
- **author:** Extracted from first article's author or URL
- **contentHash:** Empty (computed by SourcePoller as usual)

### 4. DB migration for `aggregate` column

Add nullable boolean column `aggregate` to the `sources` table. Null means auto-detect. Existing sources get `null` (no behavior change for non-twitter/nitter sources, auto-aggregation kicks in for twitter/nitter).

## Risks / Trade-offs

- **[Content hash changes on every poll]** → Since aggregated body changes each poll (different tweets), dedup via content hash won't prevent re-storing similar digests. Mitigation: This is acceptable — the `lastSeenId` cursor already prevents fetching duplicate tweets, so the aggregated body genuinely changes each poll.
- **[Loss of individual tweet granularity]** → Individual tweets can't be independently scored/filtered. Mitigation: This is the desired behavior — the LLM evaluates the batch as a whole, which is more effective for short content.
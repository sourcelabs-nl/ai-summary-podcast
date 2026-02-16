# Capability: Source Aggregation

## Purpose

Hybrid auto-detect + per-source override aggregation of short-form content items (tweets, microposts) into a single article per source per poll cycle for more effective LLM processing.

## Requirements

### Requirement: Source article aggregation
The system SHALL provide a `SourceAggregator` component that merges multiple fetched articles from a single source into one consolidated digest article. The aggregator SHALL be called after fetching and before storage. When a source has 0 or 1 articles, the aggregator SHALL return them unchanged.

#### Scenario: Multiple articles aggregated into digest
- **WHEN** a source with aggregation enabled produces 5 articles in a poll cycle
- **THEN** the aggregator merges them into a single article with all item texts joined by `\n\n---\n\n` separators

#### Scenario: Single article returned unchanged
- **WHEN** a source with aggregation enabled produces 1 article in a poll cycle
- **THEN** the aggregator returns the single article unchanged

#### Scenario: Empty article list returned unchanged
- **WHEN** a source with aggregation enabled produces 0 articles in a poll cycle
- **THEN** the aggregator returns an empty list

### Requirement: Aggregated article format
The aggregated article SHALL have the following fields:
- `title`: `"Posts from @{username} — {date}"` where username is the first article's author value (kept as-is including `@` prefix if present), or the source URL domain if no author is available. Date is formatted as `MMM d, yyyy` in English locale using UTC timezone (e.g. `Feb 16, 2026`)
- `body`: All article bodies joined with `\n\n---\n\n`, each prefixed with its `publishedAt` timestamp on a separate line if available
- `url`: The source URL
- `publishedAt`: The most recent `publishedAt` value from the batch
- `author`: The first article's author value, or `null` if no articles have an author
- `contentHash`: Empty string (computed by `SourcePoller`). Note: since the aggregated body changes each poll cycle, content hash dedup will not prevent re-storing — this is acceptable because `lastSeenId` cursor tracking prevents fetching duplicate source items

#### Scenario: Digest title with known author
- **WHEN** aggregating articles from a source where the first article has `author` = `@simonw`
- **THEN** the digest title is `"Posts from @simonw — Feb 16, 2026"` (using the most recent article's date)

#### Scenario: Digest title without author
- **WHEN** aggregating articles from a source where articles have no author field
- **THEN** the digest title uses the source URL domain, e.g. `"Posts from nitter.net — Feb 16, 2026"`

#### Scenario: Digest body format
- **WHEN** aggregating 2 articles with bodies "Hello world" (published 10:00) and "Good morning" (published 09:00)
- **THEN** the digest body contains both texts separated by `\n\n---\n\n`, each prefixed with its timestamp

#### Scenario: Digest author when first article has no author
- **WHEN** aggregating articles where the first article has `author` = `null`
- **THEN** the digest article's `author` is `null`

#### Scenario: Aggregated article stored every poll cycle
- **WHEN** an aggregated source is polled twice and produces different tweets each time
- **THEN** both digest articles are stored (content hash dedup does not prevent this because the body differs each cycle)

#### Scenario: Digest publishedAt uses most recent
- **WHEN** aggregating articles with publishedAt values "2026-02-16T09:00:00Z" and "2026-02-16T10:00:00Z"
- **THEN** the digest article's publishedAt is "2026-02-16T10:00:00Z"

### Requirement: Hybrid aggregation detection
The system SHALL determine whether to aggregate a source's articles using a hybrid approach:
1. If the source has `aggregate` = `true`, always aggregate
2. If the source has `aggregate` = `false`, never aggregate
3. If the source has `aggregate` = `null` (default), auto-detect:
   - Aggregate if source type is `"twitter"`
   - Aggregate if source URL contains `nitter.net`
   - Do not aggregate otherwise

#### Scenario: Explicit aggregate true overrides auto-detect
- **WHEN** an RSS source with URL `https://example.com/feed.xml` has `aggregate` = `true`
- **THEN** articles from this source are aggregated

#### Scenario: Explicit aggregate false overrides auto-detect
- **WHEN** a source with type `"twitter"` has `aggregate` = `false`
- **THEN** articles from this source are NOT aggregated

#### Scenario: Auto-detect aggregates twitter type
- **WHEN** a source with type `"twitter"` has `aggregate` = `null`
- **THEN** articles from this source are aggregated

#### Scenario: Auto-detect aggregates nitter URL
- **WHEN** an RSS source with URL `https://nitter.net/user/rss` has `aggregate` = `null`
- **THEN** articles from this source are aggregated

#### Scenario: Auto-detect does not aggregate regular RSS
- **WHEN** an RSS source with URL `https://example.com/feed.xml` has `aggregate` = `null`
- **THEN** articles from this source are NOT aggregated

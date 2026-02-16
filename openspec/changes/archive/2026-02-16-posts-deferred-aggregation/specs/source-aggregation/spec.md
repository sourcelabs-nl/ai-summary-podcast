## MODIFIED Requirements

### Requirement: Source article aggregation
The system SHALL provide a `SourceAggregator` component that merges multiple posts from a single source into one consolidated digest article. The aggregator SHALL be invoked during script generation (in the LLM pipeline), NOT during source polling. When a source has 0 or 1 posts, the aggregator SHALL return them as individual articles (1:1 mapping). The aggregator SHALL create entries in the `post_articles` join table linking each post to its resulting article.

#### Scenario: Multiple posts aggregated into digest article
- **WHEN** a source with aggregation enabled has 5 unlinked posts within the time window
- **THEN** the aggregator merges them into a single article with all post texts joined by `\n\n---\n\n` separators, and creates 5 `post_articles` entries

#### Scenario: Single post returned as individual article
- **WHEN** a source with aggregation enabled has 1 unlinked post within the time window
- **THEN** the aggregator creates a 1:1 article from the post with 1 `post_articles` entry

#### Scenario: No posts returns empty list
- **WHEN** a source with aggregation enabled has 0 unlinked posts within the time window
- **THEN** the aggregator returns an empty list

#### Scenario: Non-aggregated source creates individual articles
- **WHEN** a source with aggregation disabled has 3 unlinked posts
- **THEN** the aggregator creates 3 individual articles, each linked to its source post via `post_articles`

### Requirement: Time-windowed aggregation
The aggregator SHALL only include posts whose `created_at` falls within a configurable time window. The time window SHALL default to the value of `app.source.max-article-age-days` (default 7 days). Posts outside the time window SHALL NOT be included in aggregation, even if they have no `post_articles` entry.

#### Scenario: Posts within time window included
- **WHEN** aggregating with a 7-day window and 3 posts were created in the last 5 days
- **THEN** all 3 posts are included in the aggregation

#### Scenario: Posts outside time window excluded
- **WHEN** aggregating with a 7-day window and a post was created 10 days ago
- **THEN** that post is excluded from aggregation

#### Scenario: Mixed-age posts partially included
- **WHEN** aggregating with a 7-day window and 5 posts exist — 3 from the last 3 days and 2 from 10 days ago
- **THEN** only the 3 recent posts are included in the aggregation

### Requirement: Aggregated article format
The aggregated article SHALL have the following fields:
- `title`: `"Posts from @{username} — {date}"` where username is the first post's author value (kept as-is including `@` prefix if present), or the source URL domain if no author is available. Date is formatted as `MMM d, yyyy` in English locale using UTC timezone (e.g. `Feb 16, 2026`)
- `body`: All post bodies joined with `\n\n---\n\n`, each prefixed with its `publishedAt` timestamp on a separate line if available
- `url`: The source URL
- `publishedAt`: The most recent `publishedAt` value from the included posts
- `author`: The first post's author value, or `null` if no posts have an author
- `contentHash`: Computed as SHA-256 of the aggregated body

#### Scenario: Digest title with known author
- **WHEN** aggregating posts from a source where the first post has `author` = `@simonw`
- **THEN** the digest title is `"Posts from @simonw — Feb 16, 2026"` (using the most recent post's date)

#### Scenario: Digest title without author
- **WHEN** aggregating posts from a source where posts have no author field
- **THEN** the digest title uses the source URL domain, e.g. `"Posts from nitter.net — Feb 16, 2026"`

#### Scenario: Digest body format
- **WHEN** aggregating 2 posts with bodies "Hello world" (published 10:00) and "Good morning" (published 09:00)
- **THEN** the digest body contains both texts separated by `\n\n---\n\n`, each prefixed with its timestamp

### Requirement: Hybrid aggregation detection
The system SHALL determine whether to aggregate a source's posts using a hybrid approach:
1. If the source has `aggregate` = `true`, always aggregate
2. If the source has `aggregate` = `false`, never aggregate
3. If the source has `aggregate` = `null` (default), auto-detect:
   - Aggregate if source type is `"twitter"`
   - Aggregate if source URL contains `nitter.net`
   - Do not aggregate otherwise

#### Scenario: Explicit aggregate true overrides auto-detect
- **WHEN** an RSS source with URL `https://example.com/feed.xml` has `aggregate` = `true`
- **THEN** posts from this source are aggregated into a digest article

#### Scenario: Explicit aggregate false overrides auto-detect
- **WHEN** a source with type `"twitter"` has `aggregate` = `false`
- **THEN** posts from this source are NOT aggregated (each post becomes an individual article)

#### Scenario: Auto-detect aggregates twitter type
- **WHEN** a source with type `"twitter"` has `aggregate` = `null`
- **THEN** posts from this source are aggregated

#### Scenario: Auto-detect aggregates nitter URL
- **WHEN** an RSS source with URL `https://nitter.net/user/rss` has `aggregate` = `null`
- **THEN** posts from this source are aggregated

#### Scenario: Auto-detect does not aggregate regular RSS
- **WHEN** an RSS source with URL `https://example.com/feed.xml` has `aggregate` = `null`
- **THEN** posts from this source are NOT aggregated (each post becomes an individual article)

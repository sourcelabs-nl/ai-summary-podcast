## MODIFIED Requirements

### Requirement: Content hash deduplication
The system SHALL compute a SHA-256 hash of each post's body text before storing. Posts with a `(source_id, content_hash)` combination already present in the `posts` table SHALL be silently skipped. Posts whose `publishedAt` is older than the configured maximum article age (`app.source.max-article-age-days`, default 7 days) SHALL be silently skipped. Posts with `publishedAt` = null SHALL NOT be filtered by age. Additionally, for a source's first poll (`lastPolled` is null), posts with `publishedAt` before the source's `createdAt` timestamp SHALL be silently skipped. Posts with `publishedAt` = null SHALL NOT be filtered by the source's `createdAt`.

#### Scenario: Identical content from different polls
- **WHEN** two separate polls produce posts with the same body text from the same source
- **THEN** only the first post is stored; the duplicate is silently skipped

#### Scenario: Post older than max age is skipped
- **WHEN** a post has `publishedAt` older than the configured `max-article-age-days`
- **THEN** the post is not saved to the `posts` table

#### Scenario: Post with null publishedAt is saved
- **WHEN** a post has `publishedAt` = null
- **THEN** the post is saved (age cannot be determined)

#### Scenario: First poll of new source skips historical posts
- **WHEN** a source with `lastPolled = null` and `createdAt = 2026-02-17T10:00:00Z` is polled and returns a post with `publishedAt = 2026-02-15T08:00:00Z`
- **THEN** the post is silently skipped because it was published before the source was created

#### Scenario: First poll of new source accepts recent posts
- **WHEN** a source with `lastPolled = null` and `createdAt = 2026-02-17T10:00:00Z` is polled and returns a post with `publishedAt = 2026-02-17T12:00:00Z`
- **THEN** the post is saved because it was published after the source was created

#### Scenario: Subsequent polls ignore createdAt filter
- **WHEN** a source with `lastPolled = 2026-02-17T12:00:00Z` is polled and returns a post with `publishedAt = 2026-02-16T08:00:00Z`
- **THEN** the `createdAt` filter is NOT applied (only applies on first poll); the post is subject to normal max-article-age filtering only

#### Scenario: First poll with null publishedAt post is not filtered
- **WHEN** a source with `lastPolled = null` is polled and returns a post with `publishedAt = null`
- **THEN** the post is saved (createdAt filter cannot be applied without a publish date)

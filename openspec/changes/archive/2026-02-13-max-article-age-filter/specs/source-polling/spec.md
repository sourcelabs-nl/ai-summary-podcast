## MODIFIED Requirements

### Requirement: Content hash deduplication
The system SHALL compute a SHA-256 hash of each article's body text before storing. Articles with a hash already present in the content store SHALL be silently skipped. Articles whose `publishedAt` is older than the configured maximum article age (`app.source.max-article-age-days`, default 7 days) SHALL be silently skipped. Articles with `publishedAt` = null SHALL NOT be filtered by age.

#### Scenario: Identical content from different polls
- **WHEN** two separate polls produce articles with the same body text
- **THEN** only the first article is stored; the duplicate is silently skipped

#### Scenario: Article older than max age is skipped
- **WHEN** an article has `publishedAt` older than the configured `max-article-age-days`
- **THEN** the article is not saved to the content store

#### Scenario: Article within max age is saved
- **WHEN** an article has `publishedAt` within the configured `max-article-age-days`
- **THEN** the article is saved to the content store (subject to dedup)

#### Scenario: Article with null publishedAt is saved
- **WHEN** an article has `publishedAt` = null
- **THEN** the article is saved to the content store (age cannot be determined)

#### Scenario: Custom max article age configured
- **WHEN** `app.source.max-article-age-days` is set to 14
- **THEN** articles up to 14 days old are saved, and articles older than 14 days are skipped

## ADDED Requirements

### Requirement: Old unprocessed article cleanup
The system SHALL periodically delete unprocessed articles whose `publishedAt` is older than the configured maximum article age. The cleanup SHALL run as part of the source polling schedule, before sources are polled. Only articles with `is_processed` = false SHALL be deleted — processed articles are retained as historical records.

#### Scenario: Old unprocessed articles deleted
- **WHEN** the source polling scheduler runs and unprocessed articles exist with `publishedAt` older than `max-article-age-days`
- **THEN** those articles are deleted from the content store

#### Scenario: Old processed articles retained
- **WHEN** the source polling scheduler runs and processed articles exist with `publishedAt` older than `max-article-age-days`
- **THEN** those articles are not deleted (they are historical records of past episodes)

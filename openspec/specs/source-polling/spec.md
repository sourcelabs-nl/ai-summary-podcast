# Capability: Source Polling

## Purpose

Scheduled polling of configured content sources (RSS feeds and websites), extracting articles and deduplicating via content hashing.

## Requirements

### Requirement: Scheduled source polling
The system SHALL poll each enabled source on a configurable schedule using Spring's `@Scheduled`. A `SourcePollingScheduler` SHALL run on a fixed interval, iterate over all enabled sources, and poll each source whose individual `pollIntervalMinutes` has elapsed since its last poll. For source types that require per-user API keys (e.g., `"twitter"`), the scheduler SHALL resolve the podcast's owner user ID and pass it to the `SourcePoller`.

#### Scenario: Source polled when interval has elapsed
- **WHEN** the scheduler runs and a source's `pollIntervalMinutes` has elapsed since its `last_polled` timestamp
- **THEN** the source is polled for new content

#### Scenario: Source skipped when interval has not elapsed
- **WHEN** the scheduler runs and a source was polled less than `pollIntervalMinutes` ago
- **THEN** the source is skipped in this polling cycle

#### Scenario: Disabled source never polled
- **WHEN** the scheduler runs and a source has `enabled: false`
- **THEN** the source is not polled

#### Scenario: Twitter source polled with user context
- **WHEN** the scheduler runs and a source with `type: "twitter"` is due for polling
- **THEN** the scheduler resolves the podcast owner's user ID and passes it to the poller so the fetcher can look up the user's X API key

### Requirement: RSS/Atom feed polling
The system SHALL parse RSS and Atom feeds using ROME (`com.rometools:rome`). For sources with type `rss`, the system SHALL fetch the feed, extract entries published after the source's `last_seen_id` timestamp, and store each new entry as an article. The system SHALL strip HTML markup from the entry content and description using `Jsoup.parse(value).text()` before storing the article body, ensuring `article.body` is always clean plain text regardless of the feed's content format. This is safe to call on already-plain text (returns unchanged). The system SHALL extract the author from the RSS entry: use `SyndEntry.author` if non-blank, otherwise use the `name` of the first entry in `SyndEntry.authors` if available. If neither provides a non-blank value, `article.author` SHALL be null. After fetching, the system SHALL pass the articles through the `SourceAggregator` which may merge them into a single digest article based on the source's aggregation settings.

#### Scenario: New RSS entries discovered without aggregation
- **WHEN** an RSS feed contains 3 entries published after the last-seen timestamp and the source does not have aggregation enabled
- **THEN** 3 new articles are created in the content store with title, clean plain-text body, URL, published timestamp, and author (when available)

#### Scenario: Nitter RSS entries aggregated into digest
- **WHEN** a nitter RSS feed contains 5 entries published after the last-seen timestamp and the source has aggregation enabled (explicit or auto-detected)
- **THEN** the 5 entries are merged into a single digest article before storage

#### Scenario: No new entries since last poll
- **WHEN** an RSS feed contains no entries published after the last-seen timestamp
- **THEN** no new articles are created and the source's `last_polled` is still updated

#### Scenario: Feed fetch fails
- **WHEN** an RSS feed URL returns an error or is unreachable
- **THEN** the error is logged, no articles are created, and the source's `last_polled` is updated to avoid retrying immediately

#### Scenario: HTML content stripped from RSS entry
- **WHEN** an RSS entry contains body text with HTML tags like `<p>Breaking news</p><a href="...">link</a>`
- **THEN** the stored article body contains only "Breaking news link" (plain text, HTML stripped)

#### Scenario: Plain text content preserved from RSS entry
- **WHEN** an RSS entry contains a plain text description without HTML
- **THEN** the stored article body contains the text unchanged

#### Scenario: Author extracted from RSS entry
- **WHEN** an RSS entry has `author` set to "John Smith"
- **THEN** the stored article has `author` = "John Smith"

#### Scenario: Author extracted from RSS entry authors list
- **WHEN** an RSS entry has blank `author` but `authors` contains a `SyndPerson` with name "Jane Doe"
- **THEN** the stored article has `author` = "Jane Doe"

#### Scenario: No author available in RSS entry
- **WHEN** an RSS entry has no `author` and empty `authors` list
- **THEN** the stored article has `author` = null

### Requirement: Website scraping
The system SHALL scrape websites using Jsoup for sources with type `website`. The system SHALL fetch the page HTML, extract article content using a heuristic approach (select `<article>` tag or largest text-containing block), and store new content identified by content hash comparison. The system SHALL extract the author from the HTML document by checking (in order): `<meta name="author" content="...">`, `<meta property="article:author" content="...">`. The first non-blank match SHALL be used as `article.author`. If no author meta tag is found, `article.author` SHALL be null.

#### Scenario: New content detected on website
- **WHEN** a website page contains content with a hash not present in the content store
- **THEN** a new article is created with the extracted text and author (when available)

#### Scenario: No content change on website
- **WHEN** a website page content hash matches an existing article's hash
- **THEN** no new article is created

#### Scenario: Author extracted from meta name tag
- **WHEN** a website page contains `<meta name="author" content="Alice Johnson">`
- **THEN** the stored article has `author` = "Alice Johnson"

#### Scenario: Author extracted from Open Graph article:author tag
- **WHEN** a website page has no `<meta name="author">` but contains `<meta property="article:author" content="Bob Williams">`
- **THEN** the stored article has `author` = "Bob Williams"

#### Scenario: No author meta tag on website
- **WHEN** a website page has no author meta tags
- **THEN** the stored article has `author` = null

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

### Requirement: Old unprocessed article cleanup
The system SHALL periodically delete unprocessed articles whose `publishedAt` is older than the configured maximum article age. The cleanup SHALL run as part of the source polling schedule, before sources are polled. Only articles with `is_processed` = false SHALL be deleted — processed articles are retained as historical records.

#### Scenario: Old unprocessed articles deleted
- **WHEN** the source polling scheduler runs and unprocessed articles exist with `publishedAt` older than `max-article-age-days`
- **THEN** those articles are deleted from the content store

#### Scenario: Old processed articles retained
- **WHEN** the source polling scheduler runs and processed articles exist with `publishedAt` older than `max-article-age-days`
- **THEN** those articles are not deleted (they are historical records of past episodes)

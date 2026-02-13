## MODIFIED Requirements

### Requirement: RSS/Atom feed polling
The system SHALL parse RSS and Atom feeds using ROME (`com.rometools:rome`). For sources with type `rss`, the system SHALL fetch the feed, extract entries published after the source's `last_seen_id` timestamp, and store each new entry as an article. The system SHALL strip HTML markup from the entry content and description using `Jsoup.parse(value).text()` before storing the article body, ensuring `article.body` is always clean plain text regardless of the feed's content format. This is safe to call on already-plain text (returns unchanged). The system SHALL extract the author from the RSS entry: use `SyndEntry.author` if non-blank, otherwise use the `name` of the first entry in `SyndEntry.authors` if available. If neither provides a non-blank value, `article.author` SHALL be null.

#### Scenario: New RSS entries discovered
- **WHEN** an RSS feed contains 3 entries published after the last-seen timestamp
- **THEN** 3 new articles are created in the content store with title, clean plain-text body, URL, published timestamp, and author (when available)

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

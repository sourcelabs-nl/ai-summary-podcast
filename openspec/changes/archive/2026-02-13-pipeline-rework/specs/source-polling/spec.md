## MODIFIED Requirements

### Requirement: RSS/Atom feed polling
The system SHALL parse RSS and Atom feeds using ROME (`com.rometools:rome`). For sources with type `rss`, the system SHALL fetch the feed, extract entries published after the source's `last_seen_id` timestamp, and store each new entry as an article. The system SHALL strip HTML markup from the entry content and description using `Jsoup.parse(value).text()` before storing the article body, ensuring `article.body` is always clean plain text regardless of the feed's content format. This is safe to call on already-plain text (returns unchanged).

#### Scenario: New RSS entries discovered
- **WHEN** an RSS feed contains 3 entries published after the last-seen timestamp
- **THEN** 3 new articles are created in the content store with title, clean plain-text body, URL, and published timestamp

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

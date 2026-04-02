## MODIFIED Requirements

### Requirement: RSS/Atom feed polling
The system SHALL parse RSS and Atom feeds using ROME (`com.rometools:rome`). For sources with type `rss`, the system SHALL fetch the feed, extract entries published after the source's `last_seen_id` timestamp, and store each new entry as a post in the `posts` table. The system SHALL strip HTML markup from the entry content and description using `Jsoup.parse(value).text()` before storing the post body. The system SHALL extract the author from the RSS entry: use `SyndEntry.author` if non-blank, otherwise use the `name` of the first entry in `SyndEntry.authors` if available. If neither provides a non-blank value, `post.author` SHALL be null. The `SourceAggregator` SHALL NOT be called during polling, aggregation is deferred to script generation time.

For sources with type `youtube`, the system SHALL fetch the YouTube channel RSS feed using the same `RssFeedFetcher`, then enrich each post by fetching the video transcript via `YouTubeTranscriptFetcher`. If a transcript is available, the post body SHALL be replaced with the transcript text prefixed by the original RSS description (separated by a blank line). If the transcript fetch fails or returns null, the original RSS description SHALL be kept as the post body. Transcript fetches SHALL respect the configured delay between requests.

#### Scenario: New RSS entries stored as individual posts
- **WHEN** an RSS feed contains 3 entries published after the last-seen timestamp
- **THEN** 3 new posts are created in the `posts` table with title, clean plain-text body, URL, published timestamp, author (when available), and content hash

#### Scenario: Nitter RSS entries stored as individual posts
- **WHEN** a nitter RSS feed contains 5 entries published after the last-seen timestamp
- **THEN** 5 individual posts are stored in the `posts` table (aggregation happens later at script generation time)

#### Scenario: No new entries since last poll
- **WHEN** an RSS feed contains no entries published after the last-seen timestamp
- **THEN** no new posts are created and the source's `last_polled` is still updated

#### Scenario: Feed fetch fails
- **WHEN** an RSS feed URL returns an error or is unreachable
- **THEN** the error is logged, no posts are created, and the source's `last_polled` is updated to avoid retrying immediately

#### Scenario: HTML content stripped from RSS entry
- **WHEN** an RSS entry contains body text with HTML tags like `<p>Breaking news</p><a href="...">link</a>`
- **THEN** the stored post body contains only "Breaking news link" (plain text, HTML stripped)

#### Scenario: Author extracted from RSS entry
- **WHEN** an RSS entry has `author` set to "John Smith"
- **THEN** the stored post has `author` = "John Smith"

#### Scenario: YouTube post enriched with transcript
- **WHEN** a YouTube source is polled and a new video post is fetched from the RSS feed
- **THEN** the system fetches the video transcript and stores the post with the transcript as the body (prefixed by the original description)

#### Scenario: YouTube post without transcript uses RSS description
- **WHEN** a YouTube source is polled, a new video is found, but the transcript fetch returns null
- **THEN** the post is stored with the original RSS description as the body

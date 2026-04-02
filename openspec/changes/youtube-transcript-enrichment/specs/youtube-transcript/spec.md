## ADDED Requirements

### Requirement: YouTube transcript fetching
The system SHALL fetch auto-generated or manual captions from YouTube videos using the public caption endpoint. A `YouTubeTranscriptFetcher` component SHALL extract the video ID from a YouTube URL, fetch the video page HTML, parse `ytInitialPlayerResponse` JSON to locate caption tracks, select the best track for the podcast's language, fetch the transcript XML, and return the plain text transcript. If no captions are available, the fetcher SHALL return null.

#### Scenario: Transcript fetched for video with auto-generated captions
- **WHEN** a YouTube video has auto-generated English captions and the podcast language is "en"
- **THEN** the fetcher returns the full transcript as plain text with HTML entities decoded

#### Scenario: Manual captions preferred over auto-generated
- **WHEN** a YouTube video has both manual English captions and auto-generated English captions
- **THEN** the fetcher selects the manual caption track

#### Scenario: Fallback to English when podcast language unavailable
- **WHEN** a YouTube video has no captions in the podcast's language but has English captions
- **THEN** the fetcher falls back to the English caption track

#### Scenario: No captions available
- **WHEN** a YouTube video has captions disabled or no caption tracks
- **THEN** the fetcher returns null and logs a warning

#### Scenario: Video page fetch fails
- **WHEN** the YouTube video page returns an HTTP error or is unreachable
- **THEN** the fetcher returns null and logs a warning

### Requirement: Transcript text extraction from XML
The system SHALL parse the transcript XML response (`<transcript><text>` elements), decode HTML entities in each text segment, and join all segments into a single plain text string separated by spaces.

#### Scenario: XML transcript parsed to plain text
- **WHEN** the transcript XML contains `<text start="0" dur="5">Hello &amp; welcome</text><text start="5" dur="3">to the show</text>`
- **THEN** the fetcher returns "Hello & welcome to the show"

### Requirement: Video ID extraction from URL
The system SHALL extract the video ID from YouTube URLs in multiple formats: `youtube.com/watch?v=VIDEO_ID`, `youtu.be/VIDEO_ID`, and `youtube.com/shorts/VIDEO_ID`.

#### Scenario: Standard watch URL
- **WHEN** the URL is `https://www.youtube.com/watch?v=dQw4w9WgXcQ`
- **THEN** the extracted video ID is `dQw4w9WgXcQ`

#### Scenario: Short URL
- **WHEN** the URL is `https://youtu.be/dQw4w9WgXcQ`
- **THEN** the extracted video ID is `dQw4w9WgXcQ`

#### Scenario: Invalid URL
- **WHEN** the URL does not contain a recognizable YouTube video ID
- **THEN** the fetcher returns null

### Requirement: Transcript length limiting
The system SHALL truncate transcripts that exceed the configured maximum length (`app.source.youtube.max-transcript-length`, default 50000 characters). Truncation SHALL occur at the end of the transcript.

#### Scenario: Long transcript truncated
- **WHEN** a video transcript is 80,000 characters and the max is 50,000
- **THEN** the transcript is truncated to 50,000 characters

#### Scenario: Short transcript not truncated
- **WHEN** a video transcript is 5,000 characters and the max is 50,000
- **THEN** the transcript is returned in full

### Requirement: Rate-limited transcript fetching
The system SHALL wait a configurable delay (`app.source.youtube.transcript-delay-ms`, default 2000ms) between transcript fetches when enriching multiple posts in a single poll cycle.

#### Scenario: Delay between transcript fetches
- **WHEN** a YouTube source poll returns 3 new videos
- **THEN** the system fetches transcripts sequentially with a 2-second delay between each fetch

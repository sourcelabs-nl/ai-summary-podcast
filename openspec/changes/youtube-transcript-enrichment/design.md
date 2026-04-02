## Context

The app supports YouTube as a source type via `SourceType.YOUTUBE`, but the `SourcePoller` delegates YouTube sources to `RssFeedFetcher`, which only returns video titles and short descriptions from the RSS feed. YouTube channels like Fireship produce spoken content that is far richer than the RSS description. YouTube embeds caption track URLs in the video page HTML (`ytInitialPlayerResponse`), which can be fetched without an API key.

Current flow: YouTube RSS poll -> Posts (title + ~2 sentence description) -> pipeline.
Target flow: YouTube RSS poll -> Posts (title + full transcript) -> pipeline.

## Goals / Non-Goals

**Goals:**
- Enrich YouTube posts with auto-generated transcript text during source polling
- Accept user-friendly YouTube channel URLs (e.g., `@Fireship`) and resolve to RSS feed URLs
- Graceful degradation when transcripts are unavailable
- Respect YouTube rate limits with configurable delays

**Non-Goals:**
- YouTube Data API integration (no API key requirement)
- Video metadata enrichment (views, likes, duration)
- Support for private/unlisted videos
- Playlist-specific polling (channel-level only)
- Transcript translation (use captions in the video's native language)

## Decisions

### 1. Transcript fetching via YouTube's public caption endpoint

Fetch the video page HTML, extract `ytInitialPlayerResponse` JSON to find caption tracks, then fetch the transcript XML from the track's `baseUrl`. This is the same approach used by the widely-adopted `youtube-transcript-api` libraries.

**Alternatives considered:**
- YouTube Data API v3: requires a Google API key, adds a provider configuration dependency, and caption download requires OAuth. Too heavy for this use case.
- yt-dlp subprocess: external binary dependency, harder to test, and overkill for transcript-only extraction.

### 2. Use RestTemplate for HTTP requests

The project already uses `RestTemplate` via `RestTemplateBuilder` in `XClient` for Twitter API calls. Reusing this pattern keeps the implementation consistent.

**Alternatives considered:**
- Jsoup `.connect()`: good for HTML scraping but not ideal for fetching XML/JSON endpoints.
- WebClient: async/reactive, but the polling context is already sequential within host groups.

### 3. Enrich posts in SourcePoller, not in aggregation

Transcript fetching happens during polling (when new videos are discovered), not during aggregation or LLM pipeline execution. This ensures the transcript is available for content hashing and deduplication.

**Alternatives considered:**
- Lazy fetch during aggregation: would miss the content hash, causing duplicate detection to use the sparse RSS description instead of the transcript.

### 4. Channel URL resolution at source creation time

When a user adds a YouTube source with a channel URL like `https://www.youtube.com/@Fireship`, the system resolves it to the RSS feed URL (`https://www.youtube.com/feeds/videos.xml?channel_id=...`) and stores the normalized URL. This happens once at validation time, not on every poll.

### 5. Language-aware caption track selection

Select the caption track matching the podcast's `language` field. Prefer manual captions over auto-generated (`kind: "asr"`) for the same language. Fall back to English, then any available track.

## Risks / Trade-offs

- **YouTube page structure changes** -> Isolate all parsing in `YouTubeTranscriptFetcher` with clear error messages. The `ytInitialPlayerResponse` pattern has been stable for years but could change.
- **Rate limiting / IP blocking** -> Configurable delay between transcript fetches (default 2s). Exponential backoff on HTTP 429. YouTube typically allows 50-100 requests/hour from a single IP.
- **Missing captions** -> Some videos have no captions. Graceful fallback to RSS description with a warning log. The post still enters the pipeline with whatever content is available.
- **Long transcripts** -> Configurable max length (default 50,000 chars) with truncation. A 1-hour video transcript is roughly 10,000-15,000 words (~60,000 chars).
- **Legal/ToS** -> YouTube's ToS restricts automated scraping at scale. This is low-frequency (polling interval of 30+ minutes, only new videos) and similar to how RSS readers access YouTube content.

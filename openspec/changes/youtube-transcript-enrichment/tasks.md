## 1. Configuration

- [ ] 1.1 Add `YoutubeProperties` to `AppProperties.kt` with `transcriptDelayMs` (default 2000) and `maxTranscriptLength` (default 50000)
- [ ] 1.2 Add YouTube config defaults to `application.yaml` under `app.source.youtube`

## 2. YouTube Transcript Fetcher

- [ ] 2.1 Create `YouTubeTranscriptFetcher.kt` in the source package with video ID extraction from YouTube URLs (watch, youtu.be, shorts formats)
- [ ] 2.2 Implement video page fetching and `ytInitialPlayerResponse` JSON extraction using RestTemplate and Jsoup
- [ ] 2.3 Implement caption track selection: prefer manual captions over auto-generated, match podcast language with English fallback
- [ ] 2.4 Implement transcript XML fetching and parsing: decode HTML entities, join text segments into plain text, apply max length truncation
- [ ] 2.5 Add `enrichWithTranscripts` method that enriches a list of posts with transcripts, respecting the configured delay between fetches

## 3. YouTube Channel URL Resolution

- [ ] 3.1 Add YouTube URL validation in `SourceService.validateUrl()` that accepts channel URLs (`@ChannelName`, `/channel/ID`) and resolves them to RSS feed URLs
- [ ] 3.2 Accept direct RSS feed URLs (`youtube.com/feeds/videos.xml?channel_id=...`) without resolution

## 4. Source Poller Integration

- [ ] 4.1 Inject `YouTubeTranscriptFetcher` into `SourcePoller` and update the YouTube polling branch to enrich posts with transcripts after RSS fetch
- [ ] 4.2 Pass the podcast language to the transcript fetcher for language-aware caption selection

## 5. Tests

- [ ] 5.1 Unit tests for video ID extraction (standard URL, short URL, shorts URL, invalid URL)
- [ ] 5.2 Unit tests for `ytInitialPlayerResponse` parsing and caption track selection (manual preferred, language matching, fallback)
- [ ] 5.3 Unit tests for transcript XML parsing (HTML entity decoding, text joining, length truncation)
- [ ] 5.4 Unit tests for `enrichWithTranscripts` (successful enrichment, fallback on failure, delay between fetches)
- [ ] 5.5 Unit tests for YouTube channel URL resolution (channel URL, handle URL, direct RSS URL, invalid URL)
- [ ] 5.6 Run `mvn test` to verify all existing tests still pass

## Why

YouTube channels like Fireship produce high-quality technical content, but the current YouTube source type only fetches RSS metadata (video title and a short description). The actual spoken content is lost, making YouTube sources nearly useless for episode generation. By fetching auto-generated transcripts from YouTube videos, this content becomes as rich as any RSS article flowing through the pipeline.

## What Changes

- Add a `YouTubeTranscriptFetcher` component that extracts video transcripts from YouTube's public caption endpoints (no API key required)
- Enrich YouTube posts with transcript text during source polling, replacing the sparse RSS description with the full spoken content
- Accept user-friendly YouTube channel URLs (e.g., `https://www.youtube.com/@Fireship`) and auto-resolve them to RSS feed URLs
- Add configuration for transcript fetch delays and max transcript length
- Graceful fallback: if a transcript is unavailable, the original RSS description is kept

## Capabilities

### New Capabilities
- `youtube-transcript`: Fetching and enriching YouTube video posts with auto-generated transcript text during source polling

### Modified Capabilities
- `source-polling`: YouTube source polling now enriches posts with transcripts after RSS fetch
- `podcast-sources`: YouTube source URL validation now accepts channel URLs and resolves them to RSS feed URLs

## Impact

- **New file**: `YouTubeTranscriptFetcher.kt` in source package
- **Modified**: `SourcePoller.kt` (YouTube branch enriches posts with transcripts), `SourceService.kt` (YouTube URL validation/normalization), `AppProperties.kt` + `application.yaml` (YouTube config)
- **Dependencies**: No new dependencies (uses existing RestTemplate and Jsoup)
- **External**: Makes HTTP requests to youtube.com during polling (rate-limited with configurable delay)
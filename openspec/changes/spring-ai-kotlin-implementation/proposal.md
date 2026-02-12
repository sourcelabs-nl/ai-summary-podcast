## Why

This project needs an initial implementation of a self-hosted pipeline that monitors content sources (RSS feeds, websites), filters and summarizes relevant content using an LLM, converts summaries to audio via TTS, and delivers them as a podcast feed consumable by any podcast app. No code exists yet.

Spring Boot with Kotlin is chosen for its mature scheduling support, Spring AI's native OpenAI-compatible client (works with OpenRouter out of the box), strong HTTP client ecosystem, and the developer's familiarity with the JVM stack. Using Spring Boot 4.0.2, Spring AI 2.0.0-M2, and Kotlin 2.3.0 provides the latest platform with production-ready AI integration without custom HTTP boilerplate. Note: Spring AI 2.0.0 is currently at milestone 2 (GA planned May 2026), which is the version targeting Spring Boot 4.

## System Architecture

The system is a self-hosted pipeline with four stages:

1. **Source ingestion and monitoring** — Periodically poll configured sources for new content.
2. **Relevance filtering and summarization** — Use an LLM to determine if new content matches the user's topic of interest, then summarize relevant items.
3. **Audio generation** — Convert the text briefing into spoken audio via a TTS API.
4. **Delivery** — Serve the audio file and an RSS podcast feed so any podcast app can subscribe.

```
┌─────────────────┐
│  Source Config   │  (YAML: URLs, feed types, topic keywords)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Source Poller   │  Scheduled task (@Scheduled)
│                 │  - RSS/Atom feed parsing (ROME)
│                 │  - HTML article extraction (Jsoup)
└────────┬────────┘
         │  New articles (title, body, URL, timestamp)
         ▼
┌─────────────────┐
│  Content Store   │  SQLite
│                 │  - Deduplication (content hash)
│                 │  - Tracks last-seen per source
└────────┬────────┘
         │  Unprocessed articles
         ▼
┌─────────────────┐
│  LLM Processing  │  Spring AI ChatClient → OpenRouter
│                 │  Step 1: Relevance scoring (discard off-topic)
│                 │  Step 2: Per-article summary
│                 │  Step 3: Compose full briefing script
└────────┬────────┘
         │  Briefing script (plain text)
         ▼
┌─────────────────┐
│  TTS Generation  │  OpenAI TTS API
│                 │  - Split text into chunks (4096 char limit)
│                 │  - Generate audio per chunk
│                 │  - Concatenate into single MP3 (FFmpeg)
└────────┬────────┘
         │  MP3 file
         ▼
┌─────────────────┐
│  Podcast Feed    │  Static RSS 2.0 XML with enclosure tags
│  + File Server   │  Served via embedded HTTP server
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Podcast App     │  AntennaPod, Pocket Casts, etc.
│  (Android)       │  Subscribes to RSS feed URL
└─────────────────┘
```

### Source Configuration

Sources are defined in a YAML configuration file. Each source has:

- `id`: Unique identifier.
- `type`: One of `rss`, `website`.
- `url`: The feed URL or page URL.
- `poll_interval_minutes`: How often to check (default: 60).
- `enabled`: Boolean toggle.

The user's topic of interest is defined as a system-level prompt used during LLM relevance filtering.

Example configuration:

```yaml
topic: "AI engineering, LLM applications, machine learning infrastructure"

sources:
  - id: simon-willison
    type: rss
    url: https://simonwillison.net/atom/everything/
    poll_interval_minutes: 60

  - id: hacker-news
    type: rss
    url: https://hnrss.org/best
    poll_interval_minutes: 30

  - id: some-blog
    type: website
    url: https://example.com/blog
    poll_interval_minutes: 120
```

### Data Model

Three main entities stored in SQLite:

- `sources`: Source configuration and state (`last_polled`, `last_seen_id`).
- `articles`: Raw content (`id`, `source_id`, `title`, `body`, `url`, `published_at`, `content_hash`, `is_relevant`, `is_processed`).
- `episodes`: Generated briefings (`id`, `generated_at`, `script_text`, `audio_file_path`, `duration_seconds`).

### Component Specifications

**Source Poller** — Executes on a configurable schedule per source. Fetches new content since the last poll. Extracts article text (strip HTML boilerplate). Stores raw content with metadata and marks items as seen. For RSS/Atom feeds: parse the feed, extract entries published after the last-seen timestamp. For websites: fetch the page HTML, extract article content with Jsoup, diff against previously seen content hashes.

**LLM Processing** — LLM access is via OpenRouter (`https://openrouter.ai/api/v1`), which provides a single OpenAI-compatible API endpoint. Spring AI's ChatClient connects to it natively. Three sequential LLM calls per briefing cycle:

- *Call 1 — Relevance filter*: For each new article, send title + first 500 words with the topic definition. Ask for a relevance score (1-5) and a one-sentence justification. Discard items scoring below 3. Can be batched. Use a cheap, fast model (e.g., Claude Haiku, GPT-4o-mini).
- *Call 2 — Individual summaries*: For each relevant article, generate a 2-3 sentence summary.
- *Call 3 — Briefing composition*: Send all summaries with instructions to compose a coherent audio briefing script. Specify: natural spoken language, transitions between topics, target length (~1500 words for ~10 minutes of audio). Use a more capable model (e.g., Claude Sonnet, GPT-4o).

**TTS Generation** — Receives the briefing script, splits into chunks at sentence boundaries respecting the 4096 character limit, sends each chunk to the OpenAI TTS API, receives MP3 audio per chunk, concatenates all chunks into a single MP3 via FFmpeg, and calculates duration/file size for RSS metadata.

**Podcast Feed** — Generates a standard RSS 2.0 XML file with `<enclosure>` tags pointing to MP3 files. Static file regenerated each time a new episode is produced. Feed and MP3s served via the embedded Spring Boot HTTP server.

**Cleanup** — A scheduled job deletes episodes older than a configurable retention period (default: 30 days) to prevent unbounded disk usage.

### External Dependencies

| Dependency | Purpose | Required |
|---|---|---|
| OpenRouter API | LLM access for relevance filtering, summarization, script composition (OpenAI-compatible endpoint) | Yes |
| OpenAI TTS API | Converting briefing text to spoken audio | Yes |
| FFmpeg | Concatenating audio chunks into a single MP3 | Yes |
| SQLite | Storing source state, articles, and episode metadata | Yes |

## What Changes

- Create a new Spring Boot 4.0.2 + Kotlin 2.3.0 application with Maven build (Java 25)
- Implement source polling with Spring's `@Scheduled` support for RSS/Atom feeds and website scraping
- Implement LLM processing pipeline (relevance filtering → summarization → briefing composition) via Spring AI's ChatClient against OpenRouter
- Implement TTS audio generation via OpenAI TTS API with FFmpeg-based chunk concatenation
- Implement podcast feed generation (RSS 2.0 XML) and static file serving for MP3s
- Set up SQLite persistence for sources, articles, and episodes
- Provide YAML-based source configuration
- Include a scheduled cleanup job for old episodes

## Capabilities

### New Capabilities

- `source-polling`: Scheduled polling of configured content sources (RSS/Atom feeds, websites). Extracts clean article text, deduplicates via content hashing, and tracks polling state per source.
- `llm-processing`: Three-step LLM pipeline via OpenRouter — relevance filtering against a configured topic, per-article summarization, and briefing script composition. Uses Spring AI ChatClient.
- `tts-generation`: Converts briefing scripts to spoken audio via TTS API. Handles text chunking at sentence boundaries, audio generation per chunk, and FFmpeg concatenation into a single MP3.
- `podcast-feed`: Generates and serves an RSS 2.0 podcast feed with enclosure tags pointing to MP3 files. Serves audio files over HTTP. Supports episode cleanup based on retention policy.
- `content-store`: SQLite-based persistence for sources, articles, and episodes. Tracks polling state, deduplication hashes, relevance/processing flags, and episode metadata.
- `source-config`: YAML-based configuration for content sources (type, URL, poll interval, enabled flag) and topic definition for relevance filtering.

### Modified Capabilities

_(none — greenfield project)_

## Impact

- **New codebase**: Entire Spring Boot + Kotlin application created from scratch
- **Dependencies**: Spring Boot 4.0.2, Spring AI 2.0.0-M2, Kotlin 2.3.0, Rome/ROME for RSS parsing, Jsoup for HTML extraction, SQLite JDBC driver, FFmpeg (system dependency)
- **External APIs**: OpenRouter (LLM), OpenAI TTS API
- **Infrastructure**: Requires a JVM runtime (Java 25), FFmpeg installed on the host, and network access to OpenRouter + TTS endpoints
- **Configuration**: Application properties (API keys, database path) + source configuration YAML
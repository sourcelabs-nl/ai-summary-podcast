## 1. Project Setup

- [x] 1.1 Generate project using Spring Initializr (via start.spring.io HTTP API or Spring CLI): `spring initializr new --project maven-project --language kotlin --boot-version 4.0.2 --group com.aisummarypodcast --artifact ai-summary-podcast --name ai-summary-podcast --java-version 25 --dependencies web,data-jdbc --packaging jar`. Note: start.spring.io may not yet support Java 25 — Java 24 is acceptable and runs on Java 25 JVM.
- [x] 1.2 Add remaining Maven dependencies not available via initializr: spring-ai-starter-model-openai, sqlite-jdbc, rome, jsoup
- [x] 1.3 Create package structure: config, source, llm, tts, podcast, store, scheduler

## 2. Configuration

- [x] 2.1 Create application.yml with Spring AI OpenRouter config, TTS settings, briefing schedule, episode directory, retention days, and feed metadata
- [x] 2.2 Create sources.yml with example topic and source entries (RSS + website)
- [x] 2.3 Implement SourceProperties (@ConfigurationProperties binding for sources.yml with topic, list of source entries with id/type/url/pollIntervalMinutes/enabled defaults)
- [x] 2.4 Implement AppProperties (@ConfigurationProperties for app.llm, app.briefing, app.episodes, app.feed — no app.tts section needed, TTS is configured via Spring AI properties)
- [x] 2.5 Implement AiConfig (ChatClient bean from Spring AI auto-config, TTS configured via spring.ai.openai.audio.speech property overrides — no manual TTS bean needed)
- [x] 2.6 Implement SchedulingConfig (@EnableScheduling)

## 3. Content Store (SQLite + Spring Data JDBC)

- [x] 3.1 Create schema.sql with sources, articles, and episodes tables (unique constraint on content_hash). Note: do NOT include PRAGMA statements — Spring's SQL init cannot execute them. WAL mode must be configured at connection level if needed.
- [x] 3.2 Implement Source entity and SourceRepository (Spring Data JDBC). Note: all timestamp fields (lastPolled, publishedAt, generatedAt) must be `String` (ISO-8601), not `Instant` — SQLite has no native timestamp type and global Instant converters interfere with entity ID resolution.
- [x] 3.3 Implement Article entity and ArticleRepository with query methods for unprocessed/unfiltered articles
- [x] 3.4 Implement Episode entity and EpisodeRepository with query method for episodes older than retention period
- [x] 3.5 Implement SqliteDialectConfig: custom JdbcDialect bean (delegates to AnsiDialect.INSTANCE) + JdbcCustomConversions with IntegerToBooleanConverter and BooleanToIntegerConverter (SQLite stores booleans as INTEGER 0/1)

## 4. Source Polling

- [x] 4.1 Implement RssFeedFetcher: fetch and parse RSS/Atom feeds with ROME, extract entries after last-seen timestamp, return list of new articles
- [x] 4.2 Implement ContentExtractor: Jsoup-based HTML-to-text extraction (select article tag or largest text block, strip boilerplate)
- [x] 4.3 Implement WebsiteFetcher: fetch page HTML with Jsoup, extract content via ContentExtractor, return article if content hash is new
- [x] 4.4 Implement SourcePoller: orchestrator that delegates to RssFeedFetcher or WebsiteFetcher based on source type, computes SHA-256 content hash, saves articles to store, updates source polling state

## 5. LLM Processing

- [x] 5.1 Implement RelevanceFilter: send article title + first 500 words with topic to ChatClient using cheap model override, parse structured RelevanceResult (score + justification), update article is_relevant flag
- [x] 5.2 Implement ArticleSummarizer: generate 2-3 sentence summary per relevant article via ChatClient
- [x] 5.3 Implement BriefingComposer: compose all summaries into a spoken-language briefing script via ChatClient using capable model, targeting configured word count
- [x] 5.4 Implement LlmPipeline: orchestrate the three steps sequentially (filter → summarize → compose), skip if no unprocessed articles, mark articles as processed

## 6. TTS Generation

- [x] 6.1 Implement text chunking: split briefing script at sentence boundaries respecting 4096 char limit, handle edge case of very long sentences
- [x] 6.2 Implement TtsService: send each chunk to OpenAiAudioSpeechModel via `org.springframework.ai.audio.tts.TextToSpeechPrompt` (package is `audio.tts`, not `audio.speech`), collect MP3 byte arrays, handle errors
- [x] 6.3 Implement AudioConcatenator: FFmpeg wrapper via ProcessBuilder to concatenate MP3 chunks into single file, skip FFmpeg for single-chunk case
- [x] 6.4 Implement audio duration calculation for the final MP3 file
- [x] 6.5 Wire TTS pipeline end-to-end: chunking → TTS → concatenation → episode record creation in store

## 7. Podcast Feed

- [x] 7.1 Implement FeedGenerator: build RSS 2.0 XML via ROME SyndFeed API with channel metadata and episode items with enclosure tags (MP3 URL, file size, audio/mpeg type)
- [x] 7.2 Configure static resource handler to serve MP3 files from the episodes directory
- [x] 7.3 Implement feed endpoint: serve generated RSS XML at /feed.xml with content type application/rss+xml
- [x] 7.4 Implement EpisodeCleanup: scheduled job to delete episodes older than retention period (remove DB record + MP3 file + regenerate feed)

## 8. Scheduling and Pipeline Integration

- [x] 8.1 Implement SourcePollingScheduler: @Scheduled fixed-interval runner that iterates enabled sources and polls those whose pollIntervalMinutes has elapsed
- [x] 8.2 Implement BriefingGenerationScheduler: @Scheduled cron-triggered runner that executes LlmPipeline → TtsPipeline → FeedGenerator in sequence
- [x] 8.3 Verify full pipeline end-to-end: source poll → article storage → LLM filtering/summarization/composition → TTS → MP3 → feed update

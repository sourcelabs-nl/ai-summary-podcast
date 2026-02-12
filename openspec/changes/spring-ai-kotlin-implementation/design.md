## Context

This is a greenfield implementation of the AI Summary Podcast pipeline described in `proposal.md`. The system monitors content sources, filters/summarizes relevant articles via LLM, generates audio briefings via TTS, and serves them as a podcast feed. No code exists yet. The tech stack is Spring Boot 4.0.2, Kotlin 2.2.21, Java 24 (runs on Java 25 JVM), and Maven.

Spring AI 2.0.0-M2 (milestone, GA planned May 2026) provides native OpenAI-compatible ChatClient and built-in OpenAI TTS support, which maps directly to the two main external API integrations (OpenRouter for LLM, OpenAI for TTS).

## Goals / Non-Goals

**Goals:**

- Implement the full four-stage pipeline: source polling → LLM processing → TTS generation → podcast feed
- Use Spring AI's ChatClient for all LLM interactions via OpenRouter
- Use Spring AI's OpenAiAudioSpeechModel for TTS
- Support RSS/Atom and website scraping as source types (initial scope)
- Produce a single deployable Spring Boot application
- Keep the architecture simple — single module, clear package structure, no microservices

**Non-Goals:**

- Twitter/X, Reddit, YouTube source types (deferred — requires separate API integrations and authentication)
- Two-voice dialogue format (start with monologue, add later)
- Web UI for source management (YAML config file is sufficient for v1)
- Multiple topics or multiple podcast feeds
- Ollama / local LLM support
- Push notifications when episodes are ready

## Decisions

### 1. Spring AI for both LLM and TTS

**Decision:** Use `spring-ai-starter-model-openai` for both LLM (via OpenRouter) and TTS (via OpenAI directly).

**Why:** Spring AI's OpenAI starter provides `ChatClient` (fluent API for chat completions) and `OpenAiAudioSpeechModel` (TTS). Since OpenRouter is OpenAI-compatible, ChatClient works by setting `spring.ai.openai.base-url` to `https://openrouter.ai/api/v1`.

**Problem:** The single OpenAI starter can only be configured with one base URL. LLM calls go to OpenRouter, but TTS calls must go to `https://api.openai.com`.

**Solution:** Use Spring AI's property-based overrides for the audio speech subsystem. The `spring.ai.openai.audio.speech.*` properties allow setting a separate `api-key` and `base-url` for TTS while the main `spring.ai.openai.*` properties point to OpenRouter. This avoids manually creating a `OpenAiAudioSpeechModel` bean entirely — Spring AI auto-configuration handles it.

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENROUTER_API_KEY}
      base-url: https://openrouter.ai/api/v1
      audio:
        speech:
          api-key: ${OPENAI_API_KEY}
          base-url: https://api.openai.com
          options:
            model: tts-1-hd
            voice: alloy
            response-format: mp3
```

**Note:** The TTS prompt class is `org.springframework.ai.audio.tts.TextToSpeechPrompt` (package `audio.tts`, not `audio.speech`).

**Alternatives considered:**
- Manual `OpenAiAudioSpeechModel` bean → unnecessary, property overrides are cleaner
- Raw HTTP client for TTS → more boilerplate, loses Spring AI's SpeechPrompt/SpeechResponse model
- Two separate OpenAI auto-configurations → Spring AI doesn't support this out of the box

### 2. Model switching per LLM call

**Decision:** Use a single `ChatClient` instance and override the model per call via `OpenAiChatOptions`.

**Why:** The pipeline needs a cheap model (e.g., `anthropic/claude-3-haiku`) for relevance filtering and a capable model (e.g., `anthropic/claude-sonnet-4-20250514`) for briefing composition. Spring AI's ChatClient supports per-request option overrides:

```kotlin
chatClient.prompt()
    .user(prompt)
    .options(OpenAiChatOptions.builder()
        .model("anthropic/claude-3-haiku")
        .temperature(0.3)
        .build())
    .call()
    .content()
```

**Alternatives considered:**
- Multiple ChatClient beans (`@Qualifier`) → unnecessary complexity for just changing the model name
- Multiple OpenAI auto-configurations → not supported cleanly

### 3. Structured output for relevance filtering

**Decision:** Use Spring AI's structured output (entity mapping) for the relevance filter step.

**Why:** The relevance filter needs to return a score (1-5) and justification per article. Spring AI can deserialize LLM responses directly into a Kotlin data class:

```kotlin
data class RelevanceResult(val score: Int, val justification: String)

val result = chatClient.prompt()
    .user(filterPrompt)
    .call()
    .entity(RelevanceResult::class.java)
```

This is cleaner than parsing JSON manually and leverages Spring AI's built-in JSON schema generation.

### 4. SQLite with Spring Data JDBC

**Decision:** Use Spring Data JDBC with SQLite for persistence.

**Why:** SQLite is sufficient for a single-user system. Spring Data JDBC provides repository abstractions without the complexity of JPA/Hibernate. It maps well to the three tables (`sources`, `articles`, `episodes`) with simple CRUD operations.

**SQLite JDBC driver:** `org.xerial:sqlite-jdbc`

**Important: SQLite dialect is NOT included out of the box.** Spring Data JDBC does not auto-detect SQLite and will fail with "Cannot determine a dialect for sqlite". A custom `JdbcDialect` bean must be provided, delegating to `AnsiDialect.INSTANCE`:

```kotlin
@Bean
fun jdbcDialect(): JdbcDialect = object : JdbcDialect {
    private val delegate = AnsiDialect.INSTANCE
    override fun limit() = delegate.limit()
    override fun lock() = delegate.lock()
    override fun getSelectContext() = delegate.selectContext
}
```

**SQLite boolean handling:** SQLite stores booleans as INTEGER (0/1). Spring Data JDBC cannot convert `Integer` to `Boolean` by default. Custom converters must be registered via `JdbcCustomConversions`:

```kotlin
@Bean
fun jdbcCustomConversions(dialect: JdbcDialect): JdbcCustomConversions {
    return JdbcCustomConversions.of(dialect, listOf(
        IntegerToBooleanConverter(),
        BooleanToIntegerConverter()
    ))
}
```

**SQLite timestamp handling:** SQLite has no native timestamp type. Entity fields that represent timestamps must be stored as ISO-8601 `String` values (not `Instant`). Using `Instant` fields with custom global converters causes issues — Spring Data JDBC applies global converters broadly, attempting to convert entity ID fields (e.g., `"simon-willison"`) to `Instant`. The safe approach is to use `String` fields in entities and convert to/from `Instant` in service-layer code.

**Alternatives considered:**
- JPA/Hibernate → overkill for 3 simple tables, and SQLite dialect support in Hibernate is limited
- Plain JDBC with JdbcTemplate → viable but loses the convenience of repository interfaces
- H2 → in-memory by default, SQLite is better for persistence across restarts

### 5. RSS parsing with ROME

**Decision:** Use ROME (`com.rometools:rome`) for RSS/Atom feed parsing.

**Why:** ROME is the standard JVM library for RSS/Atom feeds. It handles both RSS 2.0 and Atom formats, parses entry dates, supports content modules, and is well-maintained. No viable alternative exists in the JVM ecosystem.

### 6. HTML content extraction with Jsoup

**Decision:** Use Jsoup for HTML fetching and article text extraction.

**Why:** Jsoup is the standard JVM HTML parser. For article extraction, use a heuristic approach: fetch the page, select the `<article>` tag or largest text-containing `<div>`, strip navigation/headers/footers. This won't match Python's `trafilatura` quality, but is sufficient for v1. The LLM can handle some noise in the input.

**Fallback:** If extraction quality is poor for specific sources, the LLM relevance filter will discard low-quality input naturally.

### 7. RSS feed generation

**Decision:** Use ROME's `SyndFeed` API to generate the podcast RSS 2.0 XML.

**Why:** ROME can both parse and generate RSS feeds. Using the same library for both avoids manual XML construction and ensures valid RSS 2.0 output with proper `<enclosure>` tags for MP3 files.

### 8. Audio file serving

**Decision:** Serve MP3 files and the RSS feed XML directly from Spring Boot using static resource handling.

**Why:** Spring Boot's built-in resource serving is sufficient. Configure a resource handler to serve files from a local directory (e.g., `./data/episodes/`). No need for a separate web server for v1. Caddy/Nginx can be added later as a reverse proxy for HTTPS.

### 9. Scheduling

**Decision:** Use Spring's `@Scheduled` with a fixed-delay approach for source polling, and a cron expression for the briefing generation pipeline.

**Why:** Spring's scheduling is built-in and requires no additional dependencies. Each source's poll interval is configurable. The briefing generation runs once daily (or on a configurable schedule) and processes all unprocessed articles.

**Structure:**
- `SourcePollingScheduler` — runs on a fixed interval, iterates over enabled sources, polls each one if its individual interval has elapsed
- `BriefingGenerationScheduler` — runs on a cron schedule (e.g., daily at 6 AM), triggers the full LLM → TTS → feed pipeline

### 10. Package structure

**Decision:** Flat package structure organized by capability, not by layer.

```
com.aisummarypodcast
├── AiSummaryPodcastApplication.kt
├── config/
│   ├── AiConfig.kt              # ChatClient bean
│   ├── AppProperties.kt         # App config properties
│   ├── SourceProperties.kt      # Source config binding
│   ├── SqliteDialectConfig.kt   # SQLite dialect + boolean converters
│   └── SchedulingConfig.kt      # @EnableScheduling
├── source/
│   ├── SourcePoller.kt           # Polling orchestrator
│   ├── RssFeedFetcher.kt         # RSS/Atom parsing
│   ├── WebsiteFetcher.kt         # HTML scraping
│   └── ContentExtractor.kt      # HTML → clean text
├── llm/
│   ├── LlmPipeline.kt           # Orchestrates 3-step pipeline
│   ├── RelevanceFilter.kt       # Step 1: scoring
│   ├── ArticleSummarizer.kt     # Step 2: summaries
│   └── BriefingComposer.kt      # Step 3: script
├── tts/
│   ├── TtsPipeline.kt           # End-to-end TTS orchestration
│   ├── TextChunker.kt           # Sentence-boundary text splitting
│   ├── TtsService.kt            # Spring AI TTS integration
│   ├── AudioConcatenator.kt     # FFmpeg wrapper
│   └── AudioDuration.kt         # ffprobe duration calculation
├── podcast/
│   ├── FeedGenerator.kt         # RSS 2.0 XML generation
│   ├── FeedController.kt        # GET /feed.xml endpoint
│   ├── WebConfig.kt             # Static resource handler for /episodes/**
│   └── EpisodeCleanup.kt        # Old episode deletion
├── store/
│   ├── Article.kt               # Entity
│   ├── Episode.kt               # Entity
│   ├── Source.kt                 # Entity
│   ├── ArticleRepository.kt
│   ├── EpisodeRepository.kt
│   └── SourceRepository.kt
└── scheduler/
    ├── SourcePollingScheduler.kt
    └── BriefingGenerationScheduler.kt
```

### 11. Project scaffolding with Spring CLI

**Decision:** Use Spring CLI's `initializr` command to generate the initial project structure.

**Why:** The Spring CLI (`spring initializr new`) generates a correctly structured Maven/Kotlin project from start.spring.io in one command, including pom.xml, directory layout, and the main application class. This avoids manual setup errors and ensures the project follows Spring Boot conventions. Dependencies available on the initializr (web, data-jdbc) are included at generation time; remaining dependencies (spring-ai, rome, jsoup, sqlite-jdbc) are added manually afterward.

**Reference:** https://docs.spring.io/spring-cli/reference/0.8/initializr.html

### 12. Configuration approach

**Decision:** Use `application.yml` for application settings (API keys, database, scheduling) and a separate `sources.yml` for content source definitions.

**Why:** Separating source configuration from application config allows editing sources without touching API keys or database settings. Spring's `@ConfigurationProperties` binds both files cleanly.

```yaml
# application.yml
spring:
  ai:
    openai:
      api-key: ${OPENROUTER_API_KEY}
      base-url: https://openrouter.ai/api/v1
      chat:
        options:
          model: anthropic/claude-sonnet-4-20250514
      audio:
        speech:
          api-key: ${OPENAI_API_KEY}
          base-url: https://api.openai.com
          options:
            model: tts-1-hd
            voice: alloy
            response-format: mp3
  datasource:
    url: jdbc:sqlite:./data/ai-summary-podcast.db
    driver-class-name: org.sqlite.JDBC
  sql:
    init:
      mode: always

app:
  llm:
    cheap-model: anthropic/claude-3-haiku
  briefing:
    cron: "0 0 6 * * *"  # Daily at 6 AM
    target-words: 1500
  episodes:
    directory: ./data/episodes
    retention-days: 30
  feed:
    base-url: http://localhost:8080
    title: AI Summary Podcast
    description: AI-generated audio briefings from your favourite content sources
```

**Note:** The `./data/episodes/` directory must be created before the application starts. The `app.tts` property group is not needed — TTS configuration is handled entirely by `spring.ai.openai.audio.speech.*` property overrides.

## Risks / Trade-offs

**[Spring AI 2.0.0-M2 is not GA]** → The API may change before the May 2026 GA release. Mitigation: pin the version, avoid using experimental features, upgrade when GA is released. The core ChatClient API has been stable since 1.0.

**[HTML content extraction quality on JVM]** → Jsoup-based extraction is less sophisticated than Python's trafilatura. Mitigation: the LLM handles noisy input well; relevance filtering will discard low-quality extractions. Can add source-specific CSS selectors in source config as an escape hatch.

**[TTS requires separate configuration]** → Spring AI's OpenAI auto-configuration can only point to one base URL. TTS needs a manually configured bean with the OpenAI URL. Mitigation: straightforward — one extra `@Bean` method in config. Not fragile.

**[FFmpeg as system dependency]** → FFmpeg must be installed on the host. Mitigation: available on all major platforms and Docker images. Use `ProcessBuilder` to call it. Fail fast with a clear error if not found on startup.

**[SQLite concurrent access]** → SQLite has limited write concurrency. Mitigation: this is a single-user system with sequential pipeline stages. No concurrent writes expected. Note: WAL mode PRAGMA cannot be set via Spring's `schema.sql` initialization — it must be set at connection level if needed (e.g., via connection init SQL or Hikari config).

**[JVM memory footprint]** → Spring Boot 4 + Kotlin uses ~200-400MB baseline. Mitigation: acceptable for a VPS deployment. Can tune with `-Xmx256m` if needed. GraalVM native image is an option for the future but not worth the complexity now.

## Context

The application currently operates as a single-tenant system: sources are defined in YAML config (`app.sources`), articles are globally pooled, and one podcast feed is generated at `/feed.xml`. All entities — sources, articles, episodes — are global with no concept of a user.

The codebase is a Spring Boot + Kotlin app using Spring Data JDBC with SQLite. The pipeline flow is: `SourcePollingScheduler` → `SourcePoller` → `LlmPipeline` (filter → summarize → compose) → `TtsPipeline` → `FeedGenerator`.

Currently, a single `ChatClient` bean is used for all LLM calls (via Spring AI + OpenRouter), and a single `OpenAiAudioSpeechModel` handles TTS. The LLM model is configured globally via `app.llm.cheapModel`, and TTS voice/speed are not configurable.

## Goals / Non-Goals

**Goals:**
- Introduce a user entity that owns one or more podcasts
- Each podcast has its own topic, set of sources, episodes, and RSS feed
- Each podcast can customize its LLM model, TTS voice/speed, briefing style, target length, and generation schedule
- Users can store their own API keys for LLM and TTS providers
- Move source configuration from YAML to database-driven, per-podcast REST API
- Keep the architecture simple and appropriate for a self-hosted tool with a small number of users

**Non-Goals:**
- Authentication or authorization (self-hosted, trust the network)
- Shared source pool optimization (deduplicating polls for the same URL across podcasts)
- User-facing UI (API-only management for now)
- Multi-tenancy isolation guarantees (no data leakage concerns for a personal tool)
- Supporting multiple TTS providers simultaneously (stick with OpenAI TTS for now; voice selection within that provider)

## Decisions

### 1. User identity: UUID, no auth

Users are identified by a UUID primary key with a `name` field. No authentication — this is a self-hosted personal tool. Users are created and managed via REST API.

**Alternative considered**: API keys per user for endpoint auth. Rejected — adds complexity with no real benefit for a trusted-network self-hosted app.

### 2. Per-user API key storage with encryption at rest

Users can store API keys for external providers in a `user_api_keys` table: `user_id`, `provider` (e.g., "openrouter", "openai"), `encrypted_api_key`. At pipeline runtime, the system resolves the API key: user's key for the provider → global key from application config.

API keys are encrypted at rest using AES-256-GCM. A master encryption key is provided via the `app.encryption.master-key` application property (typically set via environment variable `APP_ENCRYPTION_MASTER_KEY`). Each stored key has its own random IV (initialization vector) prepended to the ciphertext. The combined `IV + ciphertext` is Base64-encoded and stored in the `encrypted_api_key` column.

A Spring `@Component` `ApiKeyEncryptor` handles encrypt/decrypt using `javax.crypto.Cipher` with `AES/GCM/NoPadding`. Keys are decrypted on read (at pipeline runtime) and never held in memory longer than needed.

If the master key is not configured, the application SHALL fail to start with a clear error message.

**Alternative considered**: Plain text storage. Rejected — API keys are sensitive credentials. Even on a self-hosted tool, database backups, logs, or accidental exposure could leak them. AES-256-GCM adds minimal complexity with significant security benefit.

### 3. Podcast as the grouping entity

A podcast sits between user and sources. Each user can have multiple podcasts, each with its own `topic`. Sources, articles (transitively), and episodes all belong to a podcast.

**Alternative considered**: `topic` on the user with a single flat list of sources. Rejected — does not support a user who wants multiple topic-based feeds.

### 4. Per-podcast customization with sensible defaults

Each podcast has optional customization columns on the `podcasts` table. When null, the system falls back to global config defaults:

| Setting | Column | Default |
|---|---|---|
| LLM model (filter/summarize) | `llm_model` | Global `app.llm.cheapModel` (anthropic/claude-3-haiku) |
| TTS voice | `tts_voice` | `"nova"` |
| TTS speed | `tts_speed` | `1.0` |
| Briefing style | `style` | `"news-briefing"` |
| Target word count | `target_words` | Global `app.briefing.targetWords` (1500) |
| Generation schedule | `cron` | Global `app.briefing.cron` (`0 0 6 * * *` — daily at 06:00) |
| Custom instructions | `custom_instructions` | None (empty) |

**Briefing styles** map to system prompt variations in `BriefingComposer`:
- `news-briefing` — professional news anchor tone, structured with transitions (default)
- `casual` — conversational, relaxed, like chatting with a friend
- `deep-dive` — analytical, in-depth exploration, longer format
- `executive-summary` — concise, fact-focused, minimal commentary

**Custom instructions** are appended to the briefing composer prompt, allowing free-form customization (e.g., "Focus on practical implications", "Use Dutch language").

**Alternative considered**: A separate `podcast_settings` table. Rejected — adds a 1:1 join for no benefit. Nullable columns on `podcasts` with application-level defaults are simpler.

### 5. ChatClient factory for per-user API keys

The current singleton `ChatClient` bean cannot switch API keys per request. A `ChatClientFactory` component is introduced that creates `ChatClient` instances dynamically:

1. Look up the podcast's owning user
2. Look up the user's API key for the relevant provider (e.g., "openrouter")
3. If found, create a `ChatClient` with that API key; otherwise use the global `ChatClient`

The factory creates lightweight instances — Spring AI's `ChatClient.Builder` is cheap to invoke. Instances are not cached (API keys could change between runs).

For TTS, the same pattern applies: `TtsService` creates `OpenAiAudioSpeechModel` instances with the user's OpenAI API key when available, falling back to the global one. It also passes the podcast's voice and speed settings per call via `OpenAiAudioSpeechOptions`.

### 6. Source ownership: per-podcast with podcast_id FK

Each source row belongs to a podcast via a `podcast_id` foreign key on the `sources` table. If two podcasts subscribe to the same RSS feed, two separate source rows exist.

**Alternative considered**: Shared source pool with a join table. Not worth the complexity for a small-user-count self-hosted app.

### 7. Article ownership: per-source (transitively per-podcast)

Articles remain linked to a source via `source_id`. Since sources are per-podcast, articles are transitively per-podcast. The `is_relevant`, `is_processed`, and `summary` fields stay on the article — no join table needed.

The `content_hash` unique constraint changes to `UNIQUE(source_id, content_hash)` to allow the same article to exist under different podcasts' sources while still deduplicating within a single source across re-polls.

**Trade-off**: Duplicate article body storage when multiple podcasts follow the same feed. Acceptable for a self-hosted tool — disk is cheap, simplicity is valuable.

### 8. Episode ownership: per-podcast with podcast_id FK

The `episodes` table gains a `podcast_id` foreign key. Each briefing generation run produces one episode per podcast.

Audio files are stored in podcast-scoped directories: `data/episodes/{podcastId}/briefing-{timestamp}.mp3`.

### 9. Polling strategy: poll all sources independently

`SourcePollingScheduler` queries all sources from the database (across all podcasts) and polls each one based on its `poll_interval_minutes`. This replaces the current YAML-driven `sourceProperties.entries`.

The `poll_interval_minutes` and `enabled` fields move from `SourceProperties`/`SourceEntry` config to the `sources` database table.

**Trade-off**: Same URL polled multiple times if shared across podcasts. Acceptable overhead for simplicity.

### 10. Per-podcast scheduling

The current global `@Scheduled(cron = ...)` on `BriefingGenerationScheduler` is replaced by a polling-based scheduler. A fixed-rate scheduler (e.g., every 60 seconds) checks all podcasts and determines which ones are due for generation based on their individual `cron` expression and a `last_generated_at` timestamp.

Implementation: `BriefingGenerationScheduler` runs on a fixed interval, queries all podcasts, evaluates each podcast's cron expression against the current time and `last_generated_at`, and triggers the pipeline for podcasts that are due.

Spring's `CronExpression` class can be used to compute the next execution time from a cron string and compare it against the last generation time.

**Alternative considered**: Dynamic `TaskScheduler` registration (register/unregister cron tasks when podcasts are created/updated/deleted). Rejected — more complex, requires lifecycle management, and harder to reason about. The polling approach is simpler and works fine for small podcast counts.

### 11. Pipeline execution per podcast

For each podcast that is due for generation:
1. Resolve the owning user's API keys
2. `LlmPipeline.run(podcast)` — filters and summarizes articles using the podcast's model, topic, style, and user API key
3. `TtsPipeline.generate(script, podcast)` — generates audio using the podcast's voice/speed settings and user API key

`RelevanceFilter` and `ArticleSummarizer` receive the podcast's `llmModel` (or default). `BriefingComposer` receives the podcast's style, target words, and custom instructions.

### 12. REST API design

User, API key, podcast, and source management via REST:

- `POST /users` — create user (name)
- `GET /users` — list users
- `GET /users/{userId}` — get user
- `PUT /users/{userId}` — update user (name)
- `DELETE /users/{userId}` — delete user and cascade

- `GET /users/{userId}/api-keys` — list user's API keys (provider names only, not the key values)
- `PUT /users/{userId}/api-keys/{provider}` — set API key for provider (body: { apiKey })
- `DELETE /users/{userId}/api-keys/{provider}` — remove API key for provider

- `POST /users/{userId}/podcasts` — create podcast (name, topic, and optional customization fields)
- `GET /users/{userId}/podcasts` — list user's podcasts
- `GET /users/{userId}/podcasts/{podcastId}` — get podcast (includes customization settings)
- `PUT /users/{userId}/podcasts/{podcastId}` — update podcast (name, topic, customization)
- `DELETE /users/{userId}/podcasts/{podcastId}` — delete podcast and cascade

- `POST /users/{userId}/podcasts/{podcastId}/sources` — add source
- `GET /users/{userId}/podcasts/{podcastId}/sources` — list podcast's sources
- `PUT /users/{userId}/podcasts/{podcastId}/sources/{sourceId}` — update source
- `DELETE /users/{userId}/podcasts/{podcastId}/sources/{sourceId}` — remove source

- `GET /users/{userId}/podcasts/{podcastId}/feed.xml` — RSS feed for podcast
- `POST /users/{userId}/podcasts/{podcastId}/generate` — trigger manual briefing

### 13. Database schema changes

```sql
CREATE TABLE users (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL
);

CREATE TABLE user_api_keys (
    user_id TEXT NOT NULL REFERENCES users(id),
    provider TEXT NOT NULL,
    encrypted_api_key TEXT NOT NULL,
    PRIMARY KEY (user_id, provider)
);

CREATE TABLE podcasts (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id),
    name TEXT NOT NULL,
    topic TEXT NOT NULL,
    llm_model TEXT,
    tts_voice TEXT DEFAULT 'nova',
    tts_speed REAL DEFAULT 1.0,
    style TEXT DEFAULT 'news-briefing',
    target_words INTEGER,
    cron TEXT DEFAULT '0 0 6 * * *',
    custom_instructions TEXT,
    last_generated_at TEXT
);

CREATE TABLE sources (
    id TEXT PRIMARY KEY,
    podcast_id TEXT NOT NULL REFERENCES podcasts(id),
    type TEXT NOT NULL,
    url TEXT NOT NULL,
    poll_interval_minutes INTEGER NOT NULL DEFAULT 60,
    enabled INTEGER NOT NULL DEFAULT 1,
    last_polled TEXT,
    last_seen_id TEXT
);

CREATE TABLE articles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    source_id TEXT NOT NULL REFERENCES sources(id),
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    url TEXT NOT NULL,
    published_at TEXT,
    content_hash TEXT NOT NULL,
    is_relevant INTEGER,
    is_processed INTEGER NOT NULL DEFAULT 0,
    summary TEXT,
    UNIQUE(source_id, content_hash)
);

CREATE TABLE episodes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    podcast_id TEXT NOT NULL REFERENCES podcasts(id),
    generated_at TEXT NOT NULL,
    script_text TEXT NOT NULL,
    audio_file_path TEXT NOT NULL,
    duration_seconds INTEGER NOT NULL
);
```

Since this is SQLite and early-stage, the schema.sql will be rewritten rather than using incremental migrations.

### 14. Config removal

`SourceProperties` and `SourceEntry` config classes are removed. The `app.sources.topic` property is replaced by the per-podcast `topic` field. The `app.sources.entries` YAML block is no longer needed.

`AppProperties` retains `llm`, `briefing`, `episodes`, and `feed` — these remain as global fallback defaults.

## Risks / Trade-offs

- **Duplicate polling/storage**: Same URL polled N times for N podcasts subscribed to it. → Acceptable for small user/podcast counts. Can optimize later with a shared source pool if needed.
- **Sequential per-podcast pipeline**: Briefing generation runs sequentially for each podcast. → For a small number of podcasts this is fine. Could parallelize with coroutines later if needed.
- **No auth on user endpoints**: Anyone on the network can create/modify users, podcasts, and sources. → Acceptable for self-hosted. Add API key auth as a follow-up if needed.
- **Master encryption key management**: The AES-256 master key must be provided via environment variable. If lost, stored API keys become unrecoverable — users would need to re-enter them. → Document this clearly. The master key is the single secret to protect.
- **API keys visible in transit**: The PUT request body contains the raw API key. → Acceptable for self-hosted. HTTPS can be added via reverse proxy.
- **ChatClient instance creation overhead**: Creating a new ChatClient per pipeline run. → Negligible cost, Spring AI builder is lightweight.
- **SQLite concurrency**: Multiple per-podcast pipeline runs could conflict on SQLite writes. → Run sequentially (current design). SQLite WAL mode handles concurrent reads fine.
- **Schema rewrite vs migration**: Rewriting schema.sql loses existing data. → Acceptable for early-stage project. Document the breaking change.

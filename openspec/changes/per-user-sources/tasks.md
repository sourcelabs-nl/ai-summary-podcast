## 1. Database Schema

- [ ] 1.1 Rewrite `schema.sql` with new tables (`users`, `user_api_keys`, `podcasts`) and modified tables (`sources` with `podcast_id` FK, `articles` with `UNIQUE(source_id, content_hash)`, `episodes` with `podcast_id` FK)

## 2. Encryption Infrastructure

- [ ] 2.1 Add `app.encryption.master-key` to `AppProperties` and application.yaml (with startup validation that fails if not set)
- [ ] 2.2 Create `ApiKeyEncryptor` component (AES-256-GCM encrypt/decrypt with random IV per key, Base64-encoded storage)

## 3. User Management

- [ ] 3.1 Create `User` entity in `store/` package (id: UUID, name: String)
- [ ] 3.2 Create `UserRepository` (CrudRepository + findById)
- [ ] 3.3 Create `UserService` with CRUD operations and cascade delete logic (delete user's podcasts → sources → articles → episodes → audio files)
- [ ] 3.4 Create `UserController` with REST endpoints: `POST /users`, `GET /users`, `GET /users/{userId}`, `PUT /users/{userId}`, `DELETE /users/{userId}` (with request/response DTOs)

## 4. User API Keys

- [ ] 4.1 Create `UserApiKey` entity in `store/` package (userId, provider, encryptedApiKey)
- [ ] 4.2 Create `UserApiKeyRepository` (find by userId, find by userId+provider, delete by userId)
- [ ] 4.3 Create `UserApiKeyService` (set key with encryption, list providers, delete key, resolve key with fallback to global config)
- [ ] 4.4 Create `UserApiKeyController` with REST endpoints: `GET /users/{userId}/api-keys`, `PUT /users/{userId}/api-keys/{provider}`, `DELETE /users/{userId}/api-keys/{provider}`

## 5. Podcast Management

- [ ] 5.1 Create `Podcast` entity in `store/` package (id, userId, name, topic, llmModel, ttsVoice, ttsSpeed, style, targetWords, cron, customInstructions, lastGeneratedAt)
- [ ] 5.2 Create `PodcastRepository` (findByUserId, CrudRepository)
- [ ] 5.3 Create `PodcastService` with CRUD operations and cascade delete logic (delete podcast's sources → articles → episodes → audio files)
- [ ] 5.4 Create `PodcastController` with REST endpoints: `POST /users/{userId}/podcasts`, `GET /users/{userId}/podcasts`, `GET /users/{userId}/podcasts/{podcastId}`, `PUT /users/{userId}/podcasts/{podcastId}`, `DELETE /users/{userId}/podcasts/{podcastId}` (with request/response DTOs including all customization fields)

## 6. Podcast Sources (REST API)

- [ ] 6.1 Update `Source` entity to add `podcastId` field (replacing implicit global ownership)
- [ ] 6.2 Update `SourceRepository` with `findByPodcastId(podcastId)` query
- [ ] 6.3 Create `SourceService` with CRUD operations scoped to podcast (add, list, update, delete source with article cascade)
- [ ] 6.4 Create `SourceController` with REST endpoints: `POST /users/{userId}/podcasts/{podcastId}/sources`, `GET /users/{userId}/podcasts/{podcastId}/sources`, `PUT /users/{userId}/podcasts/{podcastId}/sources/{sourceId}`, `DELETE /users/{userId}/podcasts/{podcastId}/sources/{sourceId}`

## 7. Source Polling (Database-Driven)

- [ ] 7.1 Update `SourcePollingScheduler` to query all enabled sources from the database instead of `SourceProperties.entries`
- [ ] 7.2 Update `SourcePoller` to work with database `Source` entity instead of `SourceEntry` config object
- [ ] 7.3 Remove `SourceProperties` and `SourceEntry` config classes; remove `app.sources` block from YAML and delete `sources.yaml`

## 8. LLM Pipeline (Podcast-Aware)

- [ ] 8.1 Create `ChatClientFactory` component that creates `ChatClient` instances using the user's OpenRouter API key (resolved via `UserApiKeyService`) or falling back to the global `ChatClient`
- [ ] 8.2 Update `RelevanceFilter` to accept podcast topic and model, and use `ChatClientFactory` for per-podcast ChatClient
- [ ] 8.3 Update `ArticleSummarizer` to accept podcast model and use `ChatClientFactory`
- [ ] 8.4 Update `BriefingComposer` to accept podcast style, target words, custom instructions, and model; add style-specific system prompts (news-briefing, casual, deep-dive, executive-summary)
- [ ] 8.5 Update `LlmPipeline.run()` to accept a `Podcast` parameter — query articles scoped to the podcast's sources, pass podcast settings to filter/summarizer/composer

## 9. TTS Pipeline (Podcast-Aware)

- [ ] 9.1 Update `TtsService` to accept per-call voice and speed options, and create `OpenAiAudioSpeechModel` instances with the user's OpenAI API key (via `UserApiKeyService`) or global fallback
- [ ] 9.2 Update `TtsPipeline` to accept a `Podcast` parameter — use podcast's voice/speed settings, save audio to podcast-scoped directory (`data/episodes/{podcastId}/`), save episode with `podcastId`
- [ ] 9.3 Update `Episode` entity to add `podcastId` field
- [ ] 9.4 Update `EpisodeRepository` with `findByPodcastId(podcastId)` query

## 10. Briefing Generation (Per-Podcast Scheduling)

- [ ] 10.1 Update `BriefingGenerationScheduler` to a fixed-interval poller that iterates over all podcasts, evaluates each podcast's `cron` + `last_generated_at`, and triggers the pipeline for podcasts that are due
- [ ] 10.2 Update pipeline invocation to pass the podcast (with its settings and resolved API keys) through `LlmPipeline` and `TtsPipeline`
- [ ] 10.3 Add `POST /users/{userId}/podcasts/{podcastId}/generate` endpoint to `PodcastController` for manual briefing generation

## 11. Podcast Feed (Podcast-Scoped)

- [ ] 11.1 Update `FeedGenerator` to accept a `Podcast` and `User` — generate RSS feed with podcast-scoped episodes, podcast+user name in title, and podcast-scoped enclosure URLs
- [ ] 11.2 Update `FeedController` to serve feed at `GET /users/{userId}/podcasts/{podcastId}/feed.xml` (remove global `GET /feed.xml`)
- [ ] 11.3 Update `WebConfig` to serve audio from podcast-scoped directories (`/episodes/{podcastId}/**`)
- [ ] 11.4 Update `EpisodeCleanup` to operate per-podcast (delete expired episodes per podcast, scoped audio file cleanup)
- [ ] 11.5 Remove global `POST /generate` endpoint from `FeedController`

## 12. Article Repository Updates

- [ ] 12.1 Update `ArticleRepository` queries (`findUnfiltered`, `findRelevantUnprocessed`) to accept a list of source IDs (scoped to a podcast's sources) instead of returning all articles globally
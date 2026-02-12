## Why

The current system has a single global set of sources and produces one podcast feed for everyone. To support multiple listeners with different interests, sources and episodes need to be scoped per user so each person gets a personalized podcast based on their own content sources and topic. Additionally, a single user may want multiple podcasts on different topics (e.g., "AI news" and "Kotlin updates"), each with its own set of sources and feed.

## What Changes

- **BREAKING**: Sources move from YAML configuration (`app.sources.entries`) to database-driven, per-user, per-podcast configuration
- **BREAKING**: The RSS feed endpoint becomes podcast-scoped (e.g., `/users/{userId}/podcasts/{podcastId}/feed.xml`) instead of the current global `/feed.xml`
- **BREAKING**: Episodes become podcast-scoped — each podcast gets its own briefings from its own sources
- Introduce a `users` table to identify and manage users
- Introduce a `podcasts` table — each user can have multiple podcasts, each with its own `topic`
- The `sources` table gains a `podcast_id` foreign key; each podcast has its own set of sources
- The `episodes` table gains a `podcast_id` foreign key; briefings are generated per podcast
- Articles remain per-source (deduplicated by `content_hash` within a source), but relevance filtering uses the podcast's topic
- The LLM pipeline and TTS pipeline run once per podcast during scheduled briefing generation
- Source polling remains global (poll all sources across all podcasts), but relevance/summarization is per-podcast
- A REST API is added for managing users, their podcasts, and podcast sources (replacing YAML config)
- Each podcast can customize its LLM model, TTS voice/speed, briefing style, and target length — with sensible defaults
- Users can store per-provider API keys (OpenRouter, OpenAI, ElevenLabs) so different users can use their own accounts
- The LLM and TTS pipeline resolve credentials and model settings per-podcast at runtime (podcast setting → user API key → global fallback)

## Capabilities

### New Capabilities
- `user-management`: User entity, registration, and identification. Provides the user concept that podcasts are scoped to.
- `user-api-keys`: Per-user API key storage for external providers (OpenRouter, OpenAI, ElevenLabs). Enables different users to use their own provider accounts. Keys are resolved at pipeline runtime with fallback to global config.
- `podcast-management`: Podcast entity scoped to a user. Each podcast has a `topic` and groups a set of sources. A user can have multiple podcasts for different topics.
- `podcast-customization`: Per-podcast settings for LLM model, TTS voice/speed, briefing style, target word count, generation schedule (cron), and custom instructions. All settings have sensible defaults.
- `podcast-sources`: Per-podcast source configuration via REST API. Replaces the global YAML-based `app.sources` config. Each podcast defines its own sources.
- `podcast-pipeline`: Podcast-scoped LLM and TTS pipeline execution. Briefing generation iterates over all podcasts, using each podcast's customization settings and the owning user's API keys.
- `podcast-feed`: Podcast-scoped RSS feed and episode serving. Each podcast gets a unique feed URL containing only its episodes.

### Modified Capabilities
_(No existing specs to modify — this is a greenfield project with no prior specs)_

## Impact

- **Database schema**: New `users`, `user_api_keys`, and `podcasts` tables. `podcasts` includes customization columns (llm_model, tts_voice, tts_speed, style, target_words, custom_instructions). `sources` gains a `podcast_id` FK. `episodes` gains a `podcast_id` FK.
- **Config**: `SourceProperties` (YAML-based source config) is removed in favor of DB-driven per-podcast sources. `app.sources.topic` moves to the per-podcast `topic` field. Global LLM/TTS config remains as fallback defaults.
- **REST API**: New endpoints for user CRUD, user API key management, podcast CRUD (including customization), and per-podcast source management. Feed endpoint changes from `/feed.xml` to `/users/{userId}/podcasts/{podcastId}/feed.xml`.
- **Schedulers**: `SourcePollingScheduler` changes to poll all sources from the database. `BriefingGenerationScheduler` changes from a single global cron to a per-podcast scheduler that respects each podcast's configured generation schedule.
- **LLM pipeline**: `LlmPipeline`, `RelevanceFilter`, and `BriefingComposer` become podcast-aware — using the podcast's model, style, and topic, with the owning user's API key. A `ChatClientFactory` creates per-request ChatClient instances with the appropriate API key.
- **TTS pipeline**: `TtsPipeline` becomes podcast-aware — using the podcast's voice/speed settings and the owning user's TTS API key. `TtsService` accepts per-call voice and speed options.
- **File storage**: Episode audio files need podcast-scoped paths (e.g., `data/episodes/{podcastId}/`).
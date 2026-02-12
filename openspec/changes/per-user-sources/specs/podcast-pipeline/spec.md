## ADDED Requirements

### Requirement: Briefing generation uses per-podcast schedule
The `BriefingGenerationScheduler` SHALL run on a fixed interval (e.g., every 60 seconds), query all podcasts, evaluate each podcast's `cron` expression against the current time and `last_generated_at`, and trigger the pipeline for podcasts that are due.

#### Scenario: Scheduled briefing respects individual cron expressions
- **WHEN** the scheduler runs and podcast A (cron: daily 06:00, last generated yesterday) and podcast B (cron: daily 18:00, last generated today at 18:00) exist, and it is currently 06:05
- **THEN** the system triggers the pipeline for podcast A only

#### Scenario: Scheduled briefing with no podcasts
- **WHEN** the scheduler runs and no podcasts exist
- **THEN** the system completes without generating any episodes

#### Scenario: Newly created podcast runs on first check
- **WHEN** the scheduler runs and a podcast has `last_generated_at` set to null and its cron expression indicates it should have run already today
- **THEN** the system triggers the pipeline for that podcast

### Requirement: LLM pipeline scoped to podcast's sources, topic, and model
The `LlmPipeline` SHALL accept a podcast (including its customization settings). It SHALL filter and summarize only articles belonging to that podcast's sources, using the podcast's `topic` for relevance filtering and the podcast's `llm_model` (or global default) for LLM calls. It SHALL use a `ChatClient` created with the owning user's API key (or global fallback).

#### Scenario: Filter articles by podcast's sources
- **WHEN** `LlmPipeline.run(podcast)` is called
- **THEN** it queries unfiltered articles only from sources where `source.podcast_id = podcastId`, and queries relevant unprocessed articles only from those same sources

#### Scenario: Use podcast's topic for relevance filtering
- **WHEN** the relevance filter runs for a podcast with topic "Kotlin and JVM development"
- **THEN** it uses "Kotlin and JVM development" as the topic in the relevance prompt

#### Scenario: Use podcast's LLM model
- **WHEN** a podcast has `llm_model` set to `"openai/gpt-4o-mini"`
- **THEN** the LLM pipeline uses this model for relevance filtering, summarization, and script composition

#### Scenario: Use user's API key for LLM calls
- **WHEN** a podcast's owning user has an "openrouter" API key stored
- **THEN** the LLM pipeline creates a ChatClient using that API key

#### Scenario: No relevant articles for podcast
- **WHEN** `LlmPipeline.run(podcast)` is called and no relevant unprocessed articles exist for the podcast's sources
- **THEN** it returns null and no episode is generated for that podcast

### Requirement: Briefing composer uses podcast style and custom instructions
The `BriefingComposer` SHALL adapt its system prompt based on the podcast's `style` field and append any `custom_instructions`. It SHALL target the podcast's `target_words` (or global default).

#### Scenario: Compose with casual style
- **WHEN** the briefing composer runs for a podcast with style `"casual"` and target_words `800`
- **THEN** it uses a conversational tone in the prompt and targets approximately 800 words

#### Scenario: Compose with custom instructions
- **WHEN** the briefing composer runs for a podcast with custom_instructions `"Use Dutch language"`
- **THEN** the custom instructions are appended to the system prompt

#### Scenario: Compose with defaults
- **WHEN** the briefing composer runs for a podcast with no custom style, target_words, or custom_instructions
- **THEN** it uses the "news-briefing" style prompt and the global target word count

### Requirement: TTS pipeline uses podcast voice and speed settings
The `TtsPipeline` SHALL use the podcast's `tts_voice` and `tts_speed` settings when generating audio. It SHALL use a TTS model created with the owning user's OpenAI API key (or global fallback). Generated episodes SHALL be stored with a reference to the podcast.

#### Scenario: Generate episode with custom voice and speed
- **WHEN** `TtsPipeline.generate(script, podcast)` is called for a podcast with `tts_voice: "alloy"` and `tts_speed: 1.25`
- **THEN** the TTS service generates audio using the "alloy" voice at 1.25x speed

#### Scenario: Generate episode with user's TTS API key
- **WHEN** a podcast's owning user has an "openai" API key stored
- **THEN** the TTS pipeline uses that API key for TTS calls

#### Scenario: Audio stored in podcast-scoped directory
- **WHEN** `TtsPipeline.generate(script, podcast)` is called
- **THEN** the audio file is saved at `data/episodes/{podcastId}/briefing-{timestamp}.mp3` and the episode record is saved with the podcast's ID

#### Scenario: Podcast episode directory creation
- **WHEN** an episode is generated for a podcast that has no previous episodes
- **THEN** the system creates the `data/episodes/{podcastId}/` directory before saving the audio file

### Requirement: Manual briefing generation per podcast
The system SHALL allow triggering briefing generation for a specific podcast via REST API, bypassing the cron schedule.

#### Scenario: Trigger manual briefing
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/generate` request is received for an existing podcast
- **THEN** the system runs the LLM pipeline and TTS pipeline for that podcast (using its customization settings and user API keys) and returns the result

#### Scenario: Trigger manual briefing for non-existing podcast
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/generate` request is received for a podcast that does not exist
- **THEN** the system returns HTTP 404

#### Scenario: Manual briefing with no relevant articles
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/generate` request is received but the podcast has no relevant unprocessed articles
- **THEN** the system returns HTTP 200 with a message indicating no relevant articles to process

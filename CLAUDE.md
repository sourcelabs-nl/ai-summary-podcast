# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Session Startup

When starting a new session, read `llms.txt` in the project root. It contains links to the latest documentation for the core technologies (Spring Boot, Spring AI, Kotlin). Use these links to look up API usage and syntax when needed during implementation.

## Project Overview

AI Summary Podcast is a self-hosted pipeline that monitors content sources (RSS feeds, websites, Twitter/X, Reddit, YouTube), filters and summarizes relevant content using an LLM, converts summaries to audio via TTS, and delivers them as a podcast feed (RSS 2.0) consumable by any podcast app.

The project is in its early stages — `proposal.md` contains the full architectural design. No implementation language has been chosen yet.

## Architecture (Four-Stage Pipeline)

1. **Source Poller** — Scheduled polling of configured sources (RSS, website scraping, Twitter/X API, Reddit API, YouTube RSS/transcripts). Extracts clean article text.
2. **LLM Processing** — Three sequential calls via OpenRouter (OpenAI-compatible API):
   - Relevance filtering (cheap model, e.g. Haiku/GPT-4o-mini)
   - Per-article summarization
   - Briefing script composition (capable model, e.g. Sonnet/GPT-4o)
3. **TTS Generation** — Text-to-speech via OpenAI TTS / ElevenLabs / Google Cloud TTS. Chunks text at sentence boundaries, concatenates audio with FFmpeg.
4. **Podcast Feed + File Server** — Generates RSS 2.0 XML with `<enclosure>` tags, serves MP3s over HTTPS.

## Data Model

Three main entities stored in SQLite (or PostgreSQL):
- `sources` — Configuration and polling state (last_polled, last_seen_id)
- `articles` — Raw content with deduplication via content_hash, relevance/processing flags
- `episodes` — Generated briefings with script text, audio file path, duration

## External Dependencies

- **OpenRouter API** — LLM access (relevance filtering, summarization, script composition)
- **TTS API** — Audio generation (OpenAI TTS, ElevenLabs, or Google Cloud TTS)
- **FFmpeg** — Audio chunk concatenation
- **SQLite/PostgreSQL** — Persistent storage

## Running the Application

Use the provided scripts to start and stop the application:

- **Start:** `./start.sh` — runs the app in the background, logs to `app.log`, PID stored in `.app.pid`
- **Stop:** `./stop.sh` — gracefully stops the app (force-kills after 10s timeout)

Required environment variables: `OPENROUTER_API_KEY`, `OPENAI_API_KEY`, `APP_ENCRYPTION_MASTER_KEY`.

## Testing

Use **MockK** (not Mockito) for all Kotlin tests. For Spring integration tests, use `@MockkBean` from the `springmockk` library (`com.ninja-squad:springmockk`) to inject mocks into the Spring context.

## Source Configuration

Sources are defined in YAML/JSON with fields: `id`, `type` (rss/website/twitter/reddit/youtube), `url`, `poll_interval_minutes`, `enabled`. A `topic` field defines the interest area for LLM relevance filtering.

## README Structure

When updating `README.md`, maintain the following section order and structure. Do not remove, reorder, or rename sections. New sections may be added at the end before "Running Tests".

1. **Title + description** — `# AI Summary Podcast` followed by a one-paragraph project summary.
2. **How It Works** — Mermaid flowchart of the pipeline stages, followed by a numbered list explaining each stage. Ends with a note on per-user podcast customization.
3. **Prerequisites** — Bulleted list of required tools and services (Java version, FFmpeg, LLM provider options, TTS API key).
4. **Setup** — Step-by-step instructions: `.env` file creation with required variables, key generation command, explanation of fallback vs per-user provider config.
   - **Using Ollama instead of OpenRouter** — Subsection with Ollama-specific setup (pull model, configure user provider via API).
   - Start/stop commands (`./start.sh`, `./stop.sh`) and direct run command (`./mvnw spring-boot:run`).
   - Note on default port and data directory.
5. **Running Tests** — Test command (`./mvnw test`) and note on MockK usage.
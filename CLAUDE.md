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
- **X (Twitter) API** — OAuth 2.0 with PKCE for polling Twitter sources. Docs: https://docs.x.com/fundamentals/authentication/oauth-2-0/authorization-code

## Running the Application

Use the provided scripts to start and stop the application:

- **Start:** `./start.sh` — runs the app in the background, logs to `app.log`, PID stored in `.app.pid`
- **Stop:** `./stop.sh` — gracefully stops the app (force-kills after 10s timeout)

Required environment variables (managed via direnv `.envrc`): `OPENROUTER_API_KEY`, `OPENAI_API_KEY`, `APP_ENCRYPTION_MASTER_KEY`.

## Testing

Use **MockK** (not Mockito) for all Kotlin tests. For Spring integration tests, use `@MockkBean` from the `springmockk` library (`com.ninja-squad:springmockk`) to inject mocks into the Spring context. Whenever a code change breaks existing tests, those tests must be fixed as part of the same change — never leave broken tests behind.

## Source Configuration

Sources are defined in YAML/JSON with fields: `id`, `type` (rss/website/twitter/reddit/youtube), `url`, `poll_interval_minutes`, `enabled`. A `topic` field defines the interest area for LLM relevance filtering.

## Architecture Guidelines

Controllers validate input, delegate to services, and map responses — no business logic. Never duplicate logic that already exists in a service. For the full set of architectural review rules (controller hygiene, service layer, Spring Data JDBC, database consistency, testing, Jackson 3.x), see the `code-review` skill or run `/code-review`.

**Concurrency:** Use Kotlin coroutines for async/background work — never use `ExecutorService` or `java.util.concurrent` thread pools directly.

**Post-implementation check:** After every code change, validate that the architecture guidelines are respected — especially controller hygiene (no business logic, no direct repository access) and proper service layer delegation. Fix violations before considering the change complete.

## Application Restart After Changes

Whenever code changes are made to the application, always restart it (`./stop.sh` then `./start.sh`) before testing or using the new feature. Never attempt to exercise a new or modified feature against a running instance that was built from old code.

## External API Integration

When adding or modifying calls to external APIs (Inworld, ElevenLabs, OpenAI, etc.), always verify the request payload against the actual API documentation before implementing. Proto/gRPC-based APIs often use string enums (e.g., `"ON"` / `"OFF"`) rather than booleans — do not assume field types. After implementing an external API change, test it against the live API before considering the task complete.

## Production Database

The application database is at `./data/ai-summary-podcast.db`. Never query the database directly for information that is available via the application's REST API. Always use the API endpoints for production operations (generating episodes, publishing, approving, etc.). Only use direct database queries as a last resort, and always ask the user for permission before modifying the database directly.

## Frontend (Next.js Dashboard)

The frontend lives in `frontend/` and uses Next.js (App Router), shadcn/ui, and Tailwind CSS v4.

- **Theme:** Orange primary color using oklch variables following official shadcn theming docs. All CSS variables go in `globals.css` under `:root` / `.dark` using oklch format, mapped via `@theme inline`.
- **Buttons:** All action buttons use the `default` variant (orange) — never `outline` or `secondary` for action buttons. Use consistent `size="sm"` across the app. Every button must have an icon (from lucide-react) alongside its label. Only use `outline` variant for cancel/close buttons in dialogs, and `destructive` for destructive actions (e.g., Discard). Use `ghost` variant only for inline icon-only buttons (e.g., delete row in key-value editors).
- **Badges:** Status badges use `default` (orange) for most statuses, except: `outline` (white) for GENERATED, `secondary` (grey) for DISCARDED. The "Published" badge uses `default` (orange) to visually stand out.
- **Header layout:** Both podcast detail and episode detail pages follow the same header order: title + inline badges on the first line, date/schedule in `text-sm` italic on the second line, description/topic in `text-sm` on the third line. The header icon in the app bar is a Podcast icon from lucide-react next to "AI Podcast Studio".
- **Episode list columns:** #, Date, Day, Status (with Published badge), Script Model (`text-xs`), TTS Model (`text-xs`), Cost (right-aligned, formatted as dollars), Actions (action buttons + View button with `outline` variant).
- **Script rendering:** Episode scripts render in chat-bubble style using `text-sm` for body text. Monologue styles use paragraph bubbles; dialogue/interview styles use alternating left/right chat bubbles with speaker labels.
- **Dialog width:** Script viewer dialog uses near-full viewport width (`w-[90vw] !max-w-7xl`). The `!important` is needed to override shadcn's default `sm:max-w-lg`.
- **API proxy:** `next.config.ts` rewrites `/api/**` to `http://localhost:8085/**`. Update the port if the backend port changes.
- **Running:** `cd frontend && npm run dev` (use `/Users/soudmaijer/.nvm/versions/node/v22.16.0/bin/npm` to avoid the stale npm v2 at `~/node_modules/.bin/npm`).

## OpenSpec Workflow

All code changes must go through an OpenSpec change — either created before implementation (`/opsx:new`) or retroactively after implementation (`/opsx:new` covering the work done). Never implement features without a corresponding OpenSpec change.

When archiving an OpenSpec change (`/opsx:archive`), always update `README.md` to reflect any new or changed capabilities introduced by the change. Follow the README Structure rules in the `readme-structure` skill when making updates. After completing the archive, always ask the user to commit the changes with `/conventional-commits:cc`.

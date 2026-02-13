## Why

There is currently no visibility into how much each pipeline run costs in terms of LLM tokens and TTS characters. When running multiple podcasts with different models, understanding per-article and per-episode costs is essential for budgeting and optimizing model choices.

## What Changes

- Extract token usage (input/output tokens) from Spring AI `ChatResponse` after each LLM call
- Track TTS character count per episode
- Store token counts and estimated costs on articles (scoring + summarization) and episodes (composition + TTS)
- Expose cost data in existing episode and article API responses
- Add a simple cost estimation based on configurable per-model pricing

## Capabilities

### New Capabilities
- `cost-tracking`: Captures LLM token usage and TTS character counts per pipeline call, stores costs on articles and episodes, and exposes them via the API

### Modified Capabilities
- `content-store`: Add cost columns to `articles` and `episodes` tables (token counts and estimated cost)
- `llm-processing`: Extract and return token usage metadata from each LLM pipeline stage
- `tts-generation`: Track total character count sent to the TTS API

## Impact

- **Database**: New columns on `articles` and `episodes` tables (Flyway V10 migration)
- **Entities**: `Article` and `Episode` data classes gain cost-related fields
- **LLM pipeline**: `CachingChatModel` must preserve token usage from `ChatResponse`; pipeline stages return token counts alongside results
- **TTS pipeline**: `TtsService` returns character count alongside audio
- **API**: `EpisodeResponse` DTO gains cost fields; article responses (if any) gain cost fields
- **Config**: Optional model pricing configuration in `application.yaml`
## Why

When podcast settings have nullable fields that inherit system defaults (e.g., LLM models, target words, max cost), the input fields appear empty, giving users no indication of what values the system will actually use. This makes it hard to make informed decisions about overrides.

## What Changes

- Add a `GET /config/defaults` backend endpoint that exposes podcast-relevant system defaults from AppProperties
- Add placeholder text to nullable input fields in the podcast settings page showing the system default values fetched from the endpoint
- Add helper text below LLM Models editor showing resolved default model assignments
- Applies to: LLM models, max LLM cost, target words, full body threshold, max article age days

## Capabilities

### New Capabilities

- `config-defaults-api`: Backend endpoint exposing system default configuration values

### Modified Capabilities

- `frontend-podcast-settings`: Add placeholder text showing system defaults on nullable fields

## Impact

- Backend: new `ConfigController` at `src/main/kotlin/com/aisummarypodcast/config/ConfigController.kt`
- Frontend: `frontend/src/app/podcasts/[podcastId]/settings/page.tsx` fetches defaults and uses dynamic placeholders
- New TypeScript type: `PodcastDefaults` in `frontend/src/lib/types.ts`

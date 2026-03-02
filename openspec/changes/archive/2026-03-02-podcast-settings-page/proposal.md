## Why

There is no way to view or edit podcast configuration from the frontend dashboard. Users must use API calls directly to change settings like LLM models, TTS voices, pronunciation dictionaries, or scheduling. Adding a dedicated settings page makes podcast configuration accessible through the UI.

## What Changes

- Add a **Settings** button (gear icon) to each podcast card on the podcasts list page, navigating to `/podcasts/{podcastId}/settings`
- Add a **Settings** link on the podcast detail page header as a secondary entry point
- Create a new settings page at `/podcasts/[podcastId]/settings` with sub-tabs organizing all podcast fields:
  - **General**: name, topic, language, style, cron, customInstructions, requireReview
  - **LLM**: llmModels, relevanceThreshold, maxLlmCostCents, fullBodyThreshold, maxArticleAgeDays, targetWords
  - **TTS**: ttsProvider, ttsVoices, ttsSettings, speakerNames
  - **Content**: sponsor, pronunciations
  - **Integrations**: soundcloudPlaylistId
- JSON map fields (llmModels, ttsVoices, ttsSettings, speakerNames, sponsor, pronunciations) use a structured key-value row editor with add/remove capability
- One Save button for the entire form, calling `PUT /users/{userId}/podcasts/{podcastId}`
- Expand the `Podcast` TypeScript interface to include all fields returned by the API

## Capabilities

### New Capabilities
- `frontend-podcast-settings`: Frontend settings page for editing podcast configuration with tabbed layout and key-value editors for JSON map fields

### Modified Capabilities
- `frontend-dashboard`: Add settings button to podcast overview cards and settings link to podcast detail page header

## Impact

- **Frontend only** — no backend changes required, the PUT endpoint already exists
- New files: `frontend/src/app/podcasts/[podcastId]/settings/page.tsx`, possibly extracted components for key-value editor and settings form sections
- Modified files: `frontend/src/app/podcasts/page.tsx` (gear icon), `frontend/src/app/podcasts/[podcastId]/page.tsx` (settings link), `frontend/src/lib/types.ts` (expand Podcast type)

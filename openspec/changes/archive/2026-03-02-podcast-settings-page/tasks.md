## 1. Type Updates

- [x] 1.1 Expand the `Podcast` TypeScript interface in `frontend/src/lib/types.ts` to include all API fields (language, llmModels, ttsProvider, ttsVoices, ttsSettings, targetWords, cron, customInstructions, relevanceThreshold, requireReview, maxLlmCostCents, maxArticleAgeDays, fullBodyThreshold, sponsor, pronunciations, lastGeneratedAt)

## 2. Key-Value Editor Component

- [x] 2.1 Create a reusable `KeyValueEditor` component in `frontend/src/components/key-value-editor.tsx` with add/remove row functionality, key/value text inputs, and serialization to/from `Record<string, string> | null`

## 3. Settings Page

- [x] 3.1 Create the settings page at `frontend/src/app/podcasts/[podcastId]/settings/page.tsx` with data fetching, form state, and Save button calling PUT endpoint
- [x] 3.2 Implement the General sub-tab (name, topic, language, style select, cron, customInstructions textarea, requireReview checkbox)
- [x] 3.3 Implement the LLM sub-tab (llmModels key-value editor, relevanceThreshold, maxLlmCostCents, fullBodyThreshold, maxArticleAgeDays, targetWords number inputs)
- [x] 3.4 Implement the TTS sub-tab (ttsProvider select, ttsVoices key-value editor, ttsSettings key-value editor, speakerNames key-value editor)
- [x] 3.5 Implement the Content sub-tab (sponsor key-value editor, pronunciations key-value editor)
- [x] 3.6 ~~Implement the Integrations sub-tab~~ Dropped — soundcloudPlaylistId not exposed in API response/request

## 4. Navigation Entry Points

- [x] 4.1 Add gear icon button to podcast cards on the podcasts list page (`frontend/src/app/podcasts/page.tsx`) with event propagation stop to avoid navigating to detail page
- [x] 4.2 Add "Settings" link/button to the podcast detail page header (`frontend/src/app/podcasts/[podcastId]/page.tsx`)
- [x] 4.3 Display cron schedule in human-readable form on the podcast detail page header using `cronstrue` library

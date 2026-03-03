## 1. Backend Code Fixes

- [x] 1.1 Return HTTP 409 from `PodcastController.generate()` when a pending/approved episode exists (currently returns 200). Separate the "no relevant articles" case (200) from the "conflict" case (409). Update or add test.
- [x] 1.2 Add relevant article count to the batch summary log in `LlmPipeline` — change log to include `(N relevant)` in the format `[LLM] Article processing complete — 12 articles in 45.2s (8 relevant)`.
- [x] 1.3 Fix `TwitterFetcher.buildLastSeenId()` to accept the resolved X userId as a parameter and cache it in `lastSeenId` even when no tweets are returned (format: `<userId>:`). Update the call site in `TwitterFetcher.fetch()` to pass the resolved userId. Update or add test.
- [x] 1.4 Change Ollama default URL in `UserProviderConfigService.PROVIDER_DEFAULT_URLS` from `http://localhost:11434` to `http://localhost:11434/v1`. Update test if the default is asserted.

## 2. Frontend Code Fix

- [x] 2.1 Add specific 409 conflict handling in `publish-wizard.tsx` — when the publish API returns HTTP 409, display a clear message like "This episode has already been published to {target}." instead of the generic error.

## 3. Verify

- [x] 3.1 Run backend tests (`mvn test`) to ensure all code changes pass.
- [x] 3.2 Verify frontend builds without errors (`cd frontend && npm run build`).

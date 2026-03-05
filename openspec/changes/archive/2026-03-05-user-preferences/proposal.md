## Why

The backend already has a full API for managing per-user provider configs (API keys for LLM and TTS providers), but there is no frontend UI to use it. Users currently need to set API keys via environment variables or direct API calls. A user preferences page in the dashboard would make this self-service.

## What Changes

- Add a gear icon button next to the user dropdown in the header that navigates to `/settings`
- Add a new `/settings` page with two sections:
  - **Profile**: Edit the user's display name
  - **API Keys**: Unified list of all configured providers (LLM and TTS), with a wizard-style dialog to add/edit provider configs
- Display a security notice that all API keys are stored encrypted and decryption requires the application master key
- Provider selection uses a fixed dropdown of known providers, each tagged with its category (LLM or TTS)
- Multiple providers per category are supported (different podcasts may use different providers)
- Base URLs are pre-filled from known defaults and only shown/editable as an override

## Capabilities

### New Capabilities
- `frontend-user-preferences`: Frontend settings page for user profile editing and API key management with wizard-style add/edit dialogs

### Modified Capabilities
- `frontend-dashboard`: Add gear icon navigation to settings page in the header, next to the user dropdown

## Impact

- Frontend: New route `/settings`, new page component, header component modified
- No backend changes required — all APIs already exist (`GET/PUT/DELETE /users/{userId}/api-keys/*`, `PUT /users/{userId}`)

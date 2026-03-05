## Context

The backend already provides full CRUD for user provider configs at `/users/{userId}/api-keys` and user profile at `/users/{userId}`. The frontend has a user selector dropdown in the header but no settings/preferences page. All API keys are stored encrypted with AES-256-GCM using a master key.

## Goals / Non-Goals

**Goals:**
- Add a gear icon in the header next to the user dropdown that navigates to `/settings`
- Build a settings page with profile editing and API key management
- Support multiple providers per category (LLM/TTS) in a single unified list
- Provide a wizard-style dialog for adding/editing provider configs
- Show security notice about encryption

**Non-Goals:**
- No backend changes — all APIs exist
- No theme/appearance settings
- No per-podcast provider overrides (that's podcast settings)
- No custom/freeform provider entry — fixed dropdown only

## Decisions

### Settings icon in header
Place a `Settings` (lucide-react gear) icon button to the right of the user dropdown. Uses `ghost` variant with `text-primary-foreground` to match the header style. Navigates to `/settings` via Next.js `Link`.

**Alternative considered:** Dropdown menu combining user switching + settings link. Rejected — more complex, changes existing UX for user switching.

### Single-page layout with two sections
No tabs — just two stacked sections: Profile (name edit) and API Keys (provider list + add button). The page is simple enough that tabs add unnecessary navigation.

### Unified provider list
All providers (LLM and TTS) in one list with a category badge to distinguish them. This matches the user's mental model — "these are my configured API keys" — rather than forcing a LLM/TTS split.

### Wizard-style dialog for add/edit
A dialog with steps: (1) select category (LLM/TTS), (2) select provider from filtered dropdown, (3) enter API key + optional base URL override. For edit, category and provider are pre-filled and read-only — user can only change the key and base URL.

Provider dropdown is filtered by category:
- LLM: openrouter, openai, ollama
- TTS: openai, elevenlabs, inworld

Base URL is pre-filled from known defaults and only editable if the user wants to override.

### API key display
The backend never returns actual keys. The UI shows "Configured" or "Not set" status. When editing, the key field is always empty (user must re-enter to change). This is standard security practice.

### Security notice
A small info banner in the API Keys section: "All API keys are stored encrypted. Decryption requires the application master key." Uses a lock icon and muted styling.

## Risks / Trade-offs

- [Provider list may grow] → The fixed dropdown is easy to extend by adding entries to a constant. No migration needed.
- [No validation that API keys actually work] → Out of scope. User can test via podcast generation. Could add a "test connection" feature later.
- [Re-entering key on edit] → Standard practice since we can't show the existing key. Minor UX friction but good security.

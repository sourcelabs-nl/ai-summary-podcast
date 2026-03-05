## Why

All 4 tabbed pages in the frontend reset to their default tab on page reload or when navigating back. Tab state is not reflected in the URL, so users cannot bookmark or share links to specific tabs (e.g., linking directly to the Sources tab of a podcast).

## What Changes

- Sync tab state with URL query parameter (`?tab=X`) on all 4 tabbed pages
- Read tab from URL on mount, update URL when tab changes
- Default to first tab when no `?tab` param is present

Affected pages:
- `/podcasts/[podcastId]` — tabs: episodes, publications, sources
- `/podcasts/[podcastId]/settings` — tabs: general, llm, tts, content
- `/podcasts/[podcastId]/upcoming` — tabs: articles, script
- `/podcasts/[podcastId]/episodes/[episodeId]` — tabs: script, articles, publications

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `frontend-dashboard`: Add URL-synced tab state across all tabbed pages.

## Impact

- 4 frontend page components — add `useSearchParams` for tab state
- No backend changes

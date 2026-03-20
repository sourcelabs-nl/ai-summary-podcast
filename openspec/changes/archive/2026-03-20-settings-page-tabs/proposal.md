## Why

The user settings are split across two pages (`/settings` and `/settings/publishing`) with an inconsistent navigation pattern. The podcast settings page already uses a clean tabbed layout. The user settings should follow the same pattern for consistency.

## What Changes

- Merge `/settings` and `/settings/publishing` into a single tabbed page at `/settings`
- Three tabs: Profile, API Keys, Publishing
- Delete the separate `/settings/publishing` page
- Update link from podcast settings publishing tab to point to `/settings?tab=publishing`
- Use toast notifications instead of inline messages (consistent with podcast settings)

## Capabilities

### New Capabilities

_None_

### Modified Capabilities

- `frontend-user-preferences`: Restructure settings page into tabbed layout

## Impact

- **Frontend**: `settings/page.tsx` (rewrite), `settings/publishing/page.tsx` (delete), podcast settings page (update link)

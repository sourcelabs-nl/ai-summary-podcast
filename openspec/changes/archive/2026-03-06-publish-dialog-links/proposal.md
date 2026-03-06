## Why

The "Published Successfully" dialog in the publish wizard displays the raw SoundCloud URL, which is too long and overflows the dialog width. The publications tab already uses a compact "Track | Playlist" link pattern that fits well.

## What Changes

- Replace the raw URL display in the publish wizard's success step with short labeled links: "Track | Playlist"
- Derive the playlist URL from the track URL (same logic already used in `publications-tab.tsx`)

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. This is a purely cosmetic frontend change — no spec-level behavior changes.

## Impact

- `frontend/src/components/publish-wizard.tsx` — result step link rendering (lines ~183-192)
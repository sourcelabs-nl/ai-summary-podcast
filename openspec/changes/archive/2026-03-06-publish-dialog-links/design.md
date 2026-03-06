## Context

The publish wizard (`publish-wizard.tsx`) shows a raw SoundCloud URL on the success step, which overflows the `sm:max-w-md` dialog. The publications tab already solves this with compact "Track | Playlist" links.

## Goals / Non-Goals

**Goals:**
- Display "Track | Playlist" links instead of the raw URL in the publish success dialog

**Non-Goals:**
- Extracting shared link components (duplication is fine for two small usages)
- Changing the publications tab layout

## Decisions

- Replicate the same URL parsing and link pattern from `publications-tab.tsx` (lines 128-160) into the publish wizard's result step
- Parse SoundCloud username from `externalUrl` to derive the playlist URL (`/sets` path)
- For non-SoundCloud targets, show just a "Link" label

## Risks / Trade-offs

- Minor code duplication between publications tab and publish wizard — acceptable given the small size
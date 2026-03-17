## Context

The episode regeneration feature creates new episodes with the same `generatedAt` date as the source episode. When publishing a regenerated episode to SoundCloud, the old track should be replaced to avoid duplicates on the same date.

## Goals / Non-Goals

**Goals:**
- Automatically replace same-day publications when publishing to any target
- Order playlist tracks newest-first (standard podcast convention)
- Prevent TTS pronunciation guide misuse

**Non-Goals:**
- Replacing publications across different dates
- Adding an explicit "unpublish" API endpoint (handled implicitly during replacement)

## Decisions

**1. Same-day replacement scoping**
- Rationale: Only replace publications where the episode's `generatedAt` date (YYYY-MM-DD) matches. This ensures regenerated episodes replace their originals without affecting other days' publications.
- Alternative: Replace all previous publications unconditionally — rejected because it would wipe the entire podcast history.

**2. Unpublish on the publisher interface**
- Rationale: Each publisher knows how to remove its content (SoundCloud deletes the track, FTP could delete the file). Adding `unpublish` to the interface keeps it extensible.
- Default implementation throws `UnsupportedOperationException` so existing publishers without unpublish still work (the publication record is deleted regardless).

**3. Soft failure on unpublish errors**
- Rationale: If unpublishing the old track fails (e.g. already deleted), we log a warning but still proceed with publishing the new episode. The old publication record is deleted either way.

## Risks / Trade-offs

- [Risk] If an old track was already manually deleted from SoundCloud, `unpublish` returns 404 → SoundCloudClient.deleteTrack already treats 404 as success, so this is safe.

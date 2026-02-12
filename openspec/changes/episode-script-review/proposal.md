## Why

The pipeline currently generates audio immediately after composing a briefing script, with no opportunity to review or edit the script beforehand. Users want to verify and optionally tweak the LLM-generated script before spending TTS credits and generating audio.

## What Changes

- Episodes gain a status lifecycle: `PENDING_REVIEW` → `APPROVED` → `GENERATED` (or `FAILED` / `DISCARDED`)
- When a podcast has `requireReview = true`, script generation saves a pending episode instead of immediately triggering TTS
- New REST endpoints to list, view, edit, approve, and discard pending episode scripts
- Approval triggers async TTS generation
- The scheduler skips generation when a `PENDING_REVIEW` or `APPROVED` episode already exists for a podcast
- Podcasts without `requireReview` continue to auto-generate as before (backward compatible)

## Capabilities

### New Capabilities
- `episode-review`: Review workflow for episode scripts — status lifecycle, script editing, approval/discard, and async TTS trigger after approval

### Modified Capabilities
- `podcast-pipeline`: Scheduler and manual generate must branch on `requireReview` and skip when a pending episode exists
- `podcast-management`: `Podcast` entity and CRUD endpoints gain a `requireReview` field
- `content-store`: `Episode` entity gains `status` field; `audioFilePath` and `durationSeconds` become nullable

## Impact

- **Entities**: `Episode` (new `status` column, nullable audio fields), `Podcast` (new `requireReview` column)
- **DB migration**: New Flyway migration for schema changes
- **Scheduler**: `BriefingGenerationScheduler` — skip-when-pending logic + branch on `requireReview`
- **Controller**: `PodcastController.generate()` — same branching for manual trigger
- **New controller**: Episode review endpoints under `/users/{userId}/podcasts/{podcastId}/episodes`
- **Async**: TTS execution after approval needs to run outside the request thread
- **APIs**: New endpoints are additive; existing behavior unchanged when `requireReview = false`
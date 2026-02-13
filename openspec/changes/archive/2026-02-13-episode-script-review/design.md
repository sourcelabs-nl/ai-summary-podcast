## Context

The pipeline currently couples script generation and audio generation into a single synchronous flow. Both the scheduler (`BriefingGenerationScheduler`) and the manual trigger (`PodcastController.generate()`) call `llmPipeline.run()` followed immediately by `ttsPipeline.generate()`. The `Episode` entity requires non-null `audioFilePath` and `durationSeconds`, making it impossible to persist a script-only episode.

Users want to review and optionally edit the LLM-generated script before committing TTS credits to produce audio.

## Goals / Non-Goals

**Goals:**
- Allow per-podcast opt-in to script review before audio generation
- Support editing the script text while in review
- Trigger TTS asynchronously after approval
- Maintain full backward compatibility — podcasts without `requireReview` behave exactly as before

**Non-Goals:**
- Notification mechanism when a script is ready for review (future work)
- UI for reviewing scripts (API-only for now)
- Versioning or diff history of script edits

## Decisions

### 1. Episode status lifecycle over separate Draft entity

**Decision**: Add a `status` field to the existing `Episode` entity rather than introducing a separate `Draft` or `PendingScript` entity.

**Rationale**: The script and its eventual audio are fundamentally the same record at different stages. A separate entity would require migration logic (draft → episode) and duplicate fields. A status field keeps the model simple: one entity, one table, one repository.

**Statuses**: `PENDING_REVIEW`, `APPROVED`, `GENERATED`, `FAILED`, `DISCARDED`

**Alternatives considered**:
- Separate `Draft` entity: Cleaner conceptually but adds a table, repository, and migration logic between entities. Rejected for unnecessary complexity.

### 2. Nullable audio fields on Episode

**Decision**: Make `audioFilePath` and `durationSeconds` nullable (`TEXT` and `INTEGER` without `NOT NULL`). Episodes in `PENDING_REVIEW` or `APPROVED` status will have null audio fields.

**Rationale**: This is the minimal change to support the two-phase lifecycle. The feed generator already queries episodes and can filter on status to only include `GENERATED` episodes.

### 3. Per-podcast opt-in via `requireReview` boolean

**Decision**: Add `require_review` (BOOLEAN, default false) to the `podcasts` table and `Podcast` entity.

**Rationale**: Backward compatible — all existing podcasts continue auto-generating. Users opt in per podcast, which is the natural granularity since different podcasts may have different quality requirements.

### 4. Async TTS via Spring `@Async`

**Decision**: Use Spring's `@Async` annotation for the TTS generation triggered by the approve endpoint. The approve endpoint updates status to `APPROVED` and returns immediately. A separate async method runs the TTS pipeline and updates the episode status to `GENERATED` or `FAILED`.

**Rationale**: The project already uses `@EnableScheduling` in `SchedulingConfig`. Adding `@EnableAsync` to the same config is minimal effort and avoids introducing a message queue or external job system. The status field on Episode serves as the "job status" — callers can poll the episode to check progress.

**Alternatives considered**:
- Synchronous TTS on approve: Simple but blocks the HTTP request for potentially minutes. Rejected.
- Spring event + `@EventListener`: More decoupled but adds indirection for a single call site. Not worth the complexity yet.

### 5. Scheduler skip logic

**Decision**: Before running the pipeline for a podcast with `requireReview = true`, check if any episode with status `PENDING_REVIEW` or `APPROVED` exists for that podcast. If so, skip generation.

**Rationale**: Prevents piling up unreviewed scripts. The user should clear the pending script (approve or discard) before a new one is generated. This check is a simple repository query.

### 6. Episode endpoints nested under podcast

**Decision**: New endpoints at `/users/{userId}/podcasts/{podcastId}/episodes/...` for list, get, edit script, approve, and discard.

**Rationale**: Episodes are already scoped to podcasts. The existing URL pattern (`/users/{userId}/podcasts/{podcastId}/...`) is consistent with `PodcastController`. A new `EpisodeController` handles these endpoints.

## Risks / Trade-offs

- **Async failure visibility**: If TTS fails after approval, the user must poll the episode status to discover it. Acceptable for now; notification mechanism is planned for later.
- **SQLite nullable migration**: Changing `NOT NULL` columns to nullable requires creating a new table and copying data (SQLite limitation). The migration will handle this.
- **Feed generator must filter by status**: The RSS feed must only include `GENERATED` episodes. Currently it likely returns all episodes — this needs a query change.

## Migration Plan

1. Add Flyway migration `V5__add_episode_review.sql`:
   - Recreate `episodes` table with nullable `audio_file_path`, `duration_seconds`, and new `status` column (default `GENERATED` for existing rows)
   - Add `require_review` column to `podcasts` (default `false`)
2. All existing episodes get status `GENERATED` — no behavior change
3. All existing podcasts get `requireReview = false` — no behavior change
4. Rollback: reverse migration recreates original table structure (data-safe since all existing rows have non-null audio fields)
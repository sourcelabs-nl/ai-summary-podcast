## Context

Episodes are currently created at the end of the LLM pipeline. Pipeline progress is tracked via SSE events and a transient `pipelineStage` state in the frontend. This means a page refresh loses the progress indicator.

## Goals / Non-Goals

**Goals:**
- Episode row exists in DB from the moment generation starts
- Pipeline stage is persisted on the episode row
- Frontend shows GENERATING episodes inline in the episode list
- Stale GENERATING episodes are cleaned up on app startup

**Non-Goals:**
- Changing the "Next Episode" banner (keep it for article count and countdown, remove pipeline progress from it)
- Changing how regeneration works (it already has an existing episode)

## Decisions

### 1. Early episode creation

At the start of `PodcastService.generateBriefing`, create an episode with status `GENERATING` and empty script. The pipeline receives the episode ID and updates `pipelineStage` as it progresses. On completion, `EpisodeService` updates the episode with script, costs, status, etc.

### 2. Pipeline stage field

Add `pipelineStage: String?` to `Episode`. Values: `aggregating`, `scoring`, `deduplicating`, `composing`, `tts`, `null` (when not generating). This is the same set of stage names already used in SSE events.

### 3. Episode list rendering

GENERATING episodes appear as the first row in the episode list with a spinner and stage text. No action buttons (can't approve/discard/regenerate a generating episode). The row uses a subtle highlight to distinguish it from completed episodes.

### 4. Startup cleanup

On application startup, find any episodes with status `GENERATING` and transition them to `FAILED` with an error message like "Generation interrupted by application restart".

### 5. Script text handling

`scriptText` on `Episode` is currently non-null. For GENERATING episodes, use an empty string until the LLM completes. The frontend already handles empty scripts gracefully.

## Risks / Trade-offs

- **Stale episodes on crash**: Mitigated by startup cleanup marking them FAILED
- **Episode numbering**: Episode gets its ID at pipeline start rather than end, which is fine since IDs are auto-increment and chronological

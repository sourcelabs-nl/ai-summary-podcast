## Why

When an episode is being generated, the pipeline progress is shown in the "Next Episode" banner, which is disconnected from the episode list. If the page is refreshed during generation, the status is lost (it relies on SSE events). Creating the episode row early with a `GENERATING` status makes the progress visible in the episode list and persistent across page loads.

## What Changes

- Add `GENERATING` to `EpisodeStatus` enum
- Add optional `pipelineStage` field to `Episode` entity (tracks current stage: aggregating, scoring, deduplicating, composing, tts)
- Create the episode row at the start of pipeline execution with status `GENERATING`
- Update `pipelineStage` on the episode as the pipeline progresses through stages
- Transition to `PENDING_REVIEW` or `GENERATED` on completion, clear `pipelineStage`
- On failure, transition to `FAILED` with error message
- Frontend: show `GENERATING` episodes in the episode list with stage indicator and spinner
- Frontend: remove pipeline progress from the "Next Episode" banner (keep article count and countdown)
- Flyway migration to add `pipeline_stage` column
- Mark stale `GENERATING` episodes as `FAILED` on application startup

## Capabilities

### New Capabilities

_None_

### Modified Capabilities

- `podcast-pipeline`: Create episode row early with GENERATING status, update stage during pipeline
- `frontend-dashboard`: Show GENERATING episodes in episode list with stage indicator

## Impact

- **Data model**: `EpisodeStatus` enum, `Episode` entity, Flyway migration
- **Pipeline**: `PodcastService.generateBriefing`, `LlmPipeline.run`, `EpisodeService`
- **API**: Episode responses include new status and pipelineStage field
- **Frontend**: Podcast detail page episode list, SSE event handling

## 1. Data Model

- [x] 1.1 Add `GENERATING` to `EpisodeStatus` enum
- [x] 1.2 Add `pipelineStage: String?` field to `Episode` entity
- [x] 1.3 Create Flyway migration to add `pipeline_stage` column to `episodes` table

## 2. Pipeline Changes

- [x] 2.1 Add `createGeneratingEpisode` method to `EpisodeService`
- [x] 2.2 Add `updatePipelineStage` method to `EpisodeService`
- [x] 2.3 Update `PodcastService.generateBriefing`: create GENERATING episode at start, pass to pipeline
- [x] 2.4 Pipeline updates pipelineStage via onProgress callback
- [x] 2.5 Update `createEpisodeFromPipelineResult`: update existing GENERATING episode
- [x] 2.6 Rename `createFailedEpisode` to `failEpisode`: update existing GENERATING episode to FAILED
- [x] 2.7 TTS stage: set pipelineStage to "tts" before TTS, use generateForExistingEpisode

## 3. Startup Cleanup

- [x] 3.1 Add @EventListener(ApplicationReadyEvent) that marks all GENERATING episodes as FAILED

## 4. Frontend

- [x] 4.1 Update Episode TypeScript type: add `pipelineStage` field
- [x] 4.2 Update episode list: render GENERATING episodes with spinner and stage text, no action buttons
- [x] 4.3 Remove pipeline progress from "Next Episode" banner (keep article count and countdown)
- [x] 4.4 Update SSE event handling: refresh episode list on stage changes

## 5. Verification

- [x] 5.1 Fix broken tests (relaxed mocks for @EventListener, updated method signatures)
- [x] 5.2 Build backend (671 tests pass) and frontend (compiles)

## ADDED Requirements

### Requirement: Early episode creation with GENERATING status
The pipeline SHALL create an episode row with status `GENERATING` and empty script text at the start of generation, before any LLM calls. The episode's `pipelineStage` field SHALL be updated as the pipeline progresses through stages (`aggregating`, `scoring`, `deduplicating`, `composing`, `tts`). On successful completion, the episode SHALL be updated with the final script, costs, token usage, and transitioned to `PENDING_REVIEW` or `GENERATED`. On failure, the episode SHALL be transitioned to `FAILED` with an error message.

#### Scenario: Episode created at pipeline start
- **WHEN** a podcast generation is triggered
- **THEN** an episode row is created with status `GENERATING`, empty `scriptText`, and `pipelineStage` set to the first active stage

#### Scenario: Pipeline stage updates
- **WHEN** the pipeline transitions between stages (aggregating, scoring, deduplicating, composing)
- **THEN** the episode's `pipelineStage` field is updated in the database

#### Scenario: Successful completion
- **WHEN** the pipeline completes successfully
- **THEN** the episode is updated with script, costs, and status `PENDING_REVIEW` (if requireReview) or proceeds to TTS, and `pipelineStage` is set to null

#### Scenario: Pipeline failure
- **WHEN** the pipeline fails with an error
- **THEN** the existing GENERATING episode is transitioned to `FAILED` with the error message, instead of creating a new FAILED episode

### Requirement: Startup cleanup of stale GENERATING episodes
On application startup, the system SHALL find all episodes with status `GENERATING` and transition them to `FAILED` with error message "Generation interrupted by application restart".

#### Scenario: Stale episodes on startup
- **WHEN** the application starts and there are episodes with status `GENERATING`
- **THEN** they are all updated to status `FAILED` with an appropriate error message

## MODIFIED Requirements

### Requirement: Shared episode creation logic in EpisodeService
The system SHALL provide a method in `EpisodeService` that encapsulates the post-pipeline episode creation logic: saving the episode, saving episode-article links, generating a recap, and updating `lastGeneratedAt` on the podcast. Both `BriefingGenerationScheduler` and `PodcastController.generate()` SHALL delegate to this shared method instead of reimplementing the logic independently.

The system SHALL NOT advance `lastGeneratedAt` when the pipeline returns no results (null). Only the successful creation of an episode SHALL update this timestamp. This ensures that articles published before the timestamp remain visible in the upcoming view even when a pipeline run finds no relevant content to compose.

#### Scenario: Pipeline returns no results
- **WHEN** `llmPipeline.run()` returns null (no relevant articles to compose)
- **THEN** the system SHALL NOT update `lastGeneratedAt` on the podcast

#### Scenario: Pipeline returns results and episode is created
- **WHEN** `llmPipeline.run()` returns a `PipelineResult` and an episode is successfully created
- **THEN** `lastGeneratedAt` SHALL be updated to the current timestamp

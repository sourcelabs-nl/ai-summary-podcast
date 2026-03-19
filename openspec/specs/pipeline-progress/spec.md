## Purpose

Defines real-time SSE progress events emitted during the LLM episode generation pipeline, and the frontend rendering of those events on the podcast detail page.

## Requirements

### Requirement: Pipeline stage SSE events
The system SHALL emit `pipeline.progress` SSE events during episode generation at each LLM pipeline stage: `aggregating`, `scoring`, and `composing`. Each event SHALL include the stage name and relevant counts (postCount for aggregating, articleCount for scoring and composing).

#### Scenario: Aggregating stage event
- **WHEN** the LLM pipeline begins aggregating unlinked posts
- **THEN** a `pipeline.progress` event is emitted with `stage: "aggregating"` and `postCount` set to the number of unlinked posts

#### Scenario: Scoring stage event
- **WHEN** the LLM pipeline begins scoring unscored articles
- **THEN** a `pipeline.progress` event is emitted with `stage: "scoring"` and `articleCount` set to the number of articles being scored

#### Scenario: Composing stage event
- **WHEN** the LLM pipeline begins composing the briefing script
- **THEN** a `pipeline.progress` event is emitted with `stage: "composing"` and `articleCount` set to the number of relevant articles

### Requirement: Inline pipeline progress on podcast detail page
The podcast detail page "Next Episode" card SHALL display the current pipeline stage with a spinner when a `pipeline.progress` event is received. The card SHALL use a highlighted border (primary color) during active generation.

#### Scenario: Progress displayed during generation
- **WHEN** a `pipeline.progress` event with `stage: "scoring"` is received for the current podcast
- **THEN** the "Next Episode" card shows a spinner icon with "Scoring articles..." text and a primary-colored border

#### Scenario: Progress cleared after generation completes
- **WHEN** an `episode.created`, `episode.generated`, or `episode.failed` event is received after pipeline progress
- **THEN** the "Next Episode" card returns to its default state showing article counts and countdown

### Requirement: Pipeline status endpoint
The system SHALL provide a `GET /users/{userId}/podcasts/{podcastId}/pipeline-status` endpoint that returns the current pipeline stage for a podcast. An in-memory `PipelineStateTracker` component SHALL listen to `PodcastEvent` instances: it records the stage from `pipeline.progress` events and clears state on `episode.created`, `episode.generated`, or `episode.failed` events.

#### Scenario: Pipeline status when generation is active
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}/pipeline-status` request is received while the LLM pipeline is in the "composing" stage
- **THEN** the system returns HTTP 200 with `{"stage": "composing"}`

#### Scenario: Pipeline status when idle
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}/pipeline-status` request is received and no pipeline is running
- **THEN** the system returns HTTP 200 with `{"stage": null}`

### Requirement: Pipeline status fetched on page load
The podcast detail page SHALL fetch the pipeline status endpoint on initial page load alongside other data fetches (podcast, episodes, upcoming articles). If a non-null stage is returned, the "Next Episode" card SHALL immediately display the active pipeline stage with a spinner, without waiting for an SSE event.

#### Scenario: Page load during active generation
- **WHEN** the podcast detail page loads while the pipeline is in the "composing" stage
- **THEN** the "Next Episode" card immediately shows a spinner with "Composing script..." text and a primary-colored border

#### Scenario: Page load when idle
- **WHEN** the podcast detail page loads and no pipeline is running
- **THEN** the "Next Episode" card shows its default state with article counts and countdown

### Requirement: Toast notifications for pipeline progress
The system SHALL show toast notifications for `pipeline.progress` events with stage-specific messages: "Aggregating N posts...", "Scoring N articles...", "Composing episode script...".

#### Scenario: Toast shown for scoring stage
- **WHEN** a `pipeline.progress` event with `stage: "scoring"` and `articleCount: 15` is received
- **THEN** a toast notification displays "Scoring 15 articles..."

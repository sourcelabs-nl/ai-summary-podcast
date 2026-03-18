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
- **WHEN** an `episode.created` or `episode.generated` event is received after pipeline progress
- **THEN** the "Next Episode" card returns to its default state showing article counts and countdown

### Requirement: Toast notifications for pipeline progress
The system SHALL show toast notifications for `pipeline.progress` events with stage-specific messages: "Aggregating N posts...", "Scoring N articles...", "Composing episode script...".

#### Scenario: Toast shown for scoring stage
- **WHEN** a `pipeline.progress` event with `stage: "scoring"` and `articleCount: 15` is received
- **THEN** a toast notification displays "Scoring 15 articles..."

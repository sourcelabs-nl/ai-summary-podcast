## MODIFIED Requirements

### Requirement: Episode list with GENERATING status
The episode list SHALL display episodes with status `GENERATING` as the first row with a spinner icon and the current pipeline stage text (e.g., "Scoring articles..."). GENERATING episodes SHALL NOT have action buttons (approve, discard, regenerate). The row SHALL use a subtle visual indicator (e.g., primary border highlight) to distinguish it from completed episodes.

#### Scenario: GENERATING episode in list
- **WHEN** an episode has status `GENERATING` with `pipelineStage` "scoring"
- **THEN** it appears as the first row with a spinner and "Scoring articles..." text, no action buttons

#### Scenario: GENERATING episode transitions to complete
- **WHEN** a GENERATING episode's status changes to `PENDING_REVIEW` or `GENERATED` via SSE event
- **THEN** the episode list refreshes and shows the episode with its final status and action buttons

#### Scenario: Pipeline progress in Next Episode banner
- **WHEN** an episode is being generated
- **THEN** the Next Episode banner continues to show article count and countdown, but does NOT show pipeline stage progress (that is now shown in the episode row)

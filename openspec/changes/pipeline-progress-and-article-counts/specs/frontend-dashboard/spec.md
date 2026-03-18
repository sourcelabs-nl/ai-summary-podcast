## ADDED Requirements

### Requirement: Countdown timer to next generation
The podcast detail page "Next Episode" card SHALL display a live countdown timer showing time until the next scheduled cron-based generation. The countdown SHALL update every second and display in `Xh XXm XXs` format with a clock icon. The countdown SHALL be hidden during active pipeline progress.

#### Scenario: Countdown displayed for scheduled podcast
- **WHEN** a podcast has cron expression `0 7 * * *` and the current time is 06:45:00
- **THEN** the "Next Episode" card shows a clock icon with "14m 60s" counting down

#### Scenario: Countdown hidden during generation
- **WHEN** the pipeline is actively generating (pipelineStage is set)
- **THEN** the countdown timer is not displayed

### Requirement: Article and post count display
The podcast detail page "Next Episode" card SHALL display both article count and post count when they differ. The format SHALL be "N articles / M posts ready" when postCount > articleCount, or "N articles ready" when they are equal.

#### Scenario: Different article and post counts
- **WHEN** the upcoming articles API returns articleCount=47 and postCount=91
- **THEN** the card displays "47 articles / 91 posts ready"

#### Scenario: Equal article and post counts
- **WHEN** the upcoming articles API returns articleCount=15 and postCount=15
- **THEN** the card displays "15 articles ready" without the post count

## MODIFIED Requirements

### Requirement: Publish button visibility based on full publication
The episode list SHALL hide the publish button when an episode has been published to ALL configured targets (soundcloud, ftp), not just any target. The discard button SHALL remain hidden when published to any target.

#### Scenario: Episode published to all targets
- **WHEN** an episode has PUBLISHED status for both soundcloud and ftp targets
- **THEN** the publish button is not shown in the episodes list

#### Scenario: Episode published to some targets
- **WHEN** an episode has PUBLISHED status for soundcloud but not ftp
- **THEN** the publish button is shown in the episodes list

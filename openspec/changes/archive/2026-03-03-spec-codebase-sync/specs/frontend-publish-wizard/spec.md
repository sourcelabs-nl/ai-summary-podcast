## MODIFIED Requirements

### Requirement: Publish button on episodes table
The system SHALL display a "Publish" button on each episode row that has status `GENERATED` and has NOT already been published. The system SHALL fetch publication status for all episodes on page load to determine which episodes are already published. Action buttons SHALL have consistent fixed width (`w-24`).

#### Scenario: Publish button visible for unpublished GENERATED episodes
- **WHEN** an episode has status `GENERATED` and no publication with status `PUBLISHED` exists
- **THEN** a "Publish" button is displayed in the actions column

#### Scenario: Publish button hidden for already published episodes
- **WHEN** an episode has status `GENERATED` but already has a publication with status `PUBLISHED`
- **THEN** the "Publish" button SHALL NOT be displayed

#### Scenario: Publish button hidden for non-GENERATED episodes
- **WHEN** an episode has status `PENDING_REVIEW`, `APPROVED`, `FAILED`, or `DISCARDED`
- **THEN** the "Publish" button SHALL NOT be displayed

### Requirement: Publish wizard dialog
The system SHALL provide a multi-step wizard dialog for publishing an episode to an external platform. The wizard SHALL have three steps: target selection, confirmation, and result. The confirmation step SHALL display the podcast name, episode number, generated date, selected target, and the episode summary (recap) if available.

#### Scenario: Open publish wizard
- **WHEN** user clicks the "Publish" button on an episode row in the episodes table
- **THEN** a dialog opens showing step 1 (target selection) with the episode number displayed

#### Scenario: Select target and confirm
- **WHEN** user selects a target (e.g., "SoundCloud") and clicks "Next"
- **THEN** the wizard advances to step 2 showing a confirmation view with the podcast name, episode number, generated date, selected target, and episode summary (if available)

#### Scenario: Publish success
- **WHEN** user clicks "Publish" on the confirmation step and the API call succeeds
- **THEN** the wizard advances to step 3 showing a success message with the external URL as a clickable link, and a "Done" button to close the dialog

#### Scenario: Publish failure
- **WHEN** user clicks "Publish" on the confirmation step and the API call fails
- **THEN** the wizard advances to step 3 showing an error message with the failure reason, and a "Done" button to close the dialog

#### Scenario: Already published conflict
- **WHEN** the API returns HTTP 409 (already published to this target)
- **THEN** the wizard shows a specific error message indicating the episode is already published to this target

#### Scenario: Close wizard refreshes data
- **WHEN** user closes the wizard dialog (via "Done" button or clicking outside)
- **THEN** the episodes list and publications data SHALL be refreshed

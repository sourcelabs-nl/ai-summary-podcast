## MODIFIED Requirements

### Requirement: Episode list page
The system SHALL display a list of episodes for a podcast at `/podcasts/{podcastId}` within a tabbed layout, fetched from `GET /users/{userId}/podcasts/{podcastId}/episodes`. Each episode row SHALL be a clickable link that navigates to `/podcasts/{podcastId}/episodes/{episodeId}`. Each episode row SHALL show the episode ID, generated date, day of week (short format), status badge, and action buttons. The episodes list SHALL be displayed under the "Episodes" tab, which is the default active tab. A "Publications" tab SHALL be displayed alongside it. The Date column SHALL have a fixed width. All badge text SHALL be consistently lowercased. The podcast detail header SHALL display the podcast name with the style badge inline next to it, and a "Settings" button right-aligned. The podcast detail header SHALL display the cron schedule in human-readable form below the topic, prefixed with "Generates" (e.g., "Generates at 03:00 PM, Monday through Friday"), at the same text size as the topic description.

#### Scenario: Click episode row navigates to detail page
- **WHEN** user clicks on an episode row
- **THEN** the app navigates to `/podcasts/{podcastId}/episodes/{episodeId}`

#### Scenario: Display episodes with columns
- **WHEN** the episode list page loads
- **THEN** episodes are displayed under the "Episodes" tab with columns: #, Date (fixed width), Day (short weekday name in muted text, e.g., "Mon"), Status (badge), and Actions

#### Scenario: Day column shows weekday
- **WHEN** episodes are displayed
- **THEN** each episode row shows the short weekday name (e.g., "Mon", "Tue") derived from the generated date

#### Scenario: Status badge for discarded episodes
- **WHEN** an episode has status `DISCARDED`
- **THEN** the status badge uses the `secondary` variant (muted grey) instead of the `default` variant

#### Scenario: Status badge for active statuses
- **WHEN** an episode has status `GENERATED`, `PENDING_REVIEW`, `APPROVED`, or `FAILED`
- **THEN** the status badge uses the `default` variant (orange)

#### Scenario: Filter episodes by status
- **WHEN** user selects a status from the filter dropdown
- **THEN** only episodes matching that status are displayed (using `?status=` query param)

#### Scenario: Show all episodes
- **WHEN** user clears the status filter
- **THEN** all episodes for the podcast are displayed

#### Scenario: Tabbed layout with Episodes and Publications
- **WHEN** user navigates to `/podcasts/{podcastId}`
- **THEN** the page displays two tabs: "Episodes" (default active) and "Publications"

#### Scenario: Action buttons on episode rows
- **WHEN** an episode has status `PENDING_REVIEW`
- **THEN** "Approve" and "Discard" buttons are displayed in the row (these perform actions without navigating away)

#### Scenario: Publish button on unpublished GENERATED episodes
- **WHEN** an episode has status `GENERATED` and has not been published
- **THEN** a "Publish" button is displayed in the row

#### Scenario: Settings button in podcast header
- **WHEN** the podcast detail page loads
- **THEN** the header area displays the podcast name with the style badge inline next to it, and a "Settings" outline button right-aligned that navigates to `/podcasts/{podcastId}/settings`

#### Scenario: Cron schedule display
- **WHEN** the podcast detail page loads and the podcast has a cron schedule
- **THEN** the header area displays the cron expression converted to human-readable text below the topic, prefixed with "Generates" (e.g., "Generates at 03:00 PM, Monday through Friday"), at the same text size as the topic description, using the `cronstrue` library

## REMOVED Requirements

### Requirement: Script viewer dialog
**Reason**: Replaced by the Script tab on the episode detail page (`/podcasts/{podcastId}/episodes/{episodeId}`). The "Script" button is removed from episode rows; clicking the row navigates to the detail page where the Script tab is the default.
**Migration**: Navigate to the episode detail page to view scripts instead of opening a modal dialog.

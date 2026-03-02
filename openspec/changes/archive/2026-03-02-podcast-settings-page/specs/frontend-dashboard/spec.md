## MODIFIED Requirements

### Requirement: Podcast overview page
The system SHALL display a vertical list of all podcasts for the selected user at the `/podcasts` route, fetched from `GET /users/{userId}/podcasts`. Each podcast row SHALL show the podcast name, an orange style badge (default variant), and topic aligned to the right. Each podcast card SHALL also display a gear icon button (Settings) that navigates to `/podcasts/{podcastId}/settings` without triggering navigation to the podcast detail page.

#### Scenario: Display podcasts
- **WHEN** a user is selected and the podcasts page loads
- **THEN** all podcasts for that user are displayed in a single-column list with name, orange style badge, topic, and a gear icon button

#### Scenario: Navigate to episode list
- **WHEN** user clicks on a podcast row (not on the gear icon)
- **THEN** the app navigates to `/podcasts/{podcastId}` showing that podcast's episodes

#### Scenario: Navigate to settings via gear icon
- **WHEN** user clicks the gear icon button on a podcast card
- **THEN** the app navigates to `/podcasts/{podcastId}/settings` and the click does NOT trigger navigation to the podcast detail page

#### Scenario: No podcasts
- **WHEN** the selected user has no podcasts
- **THEN** an empty state message is displayed

### Requirement: Episode list page
The system SHALL display a list of episodes for a podcast at `/podcasts/{podcastId}` within a tabbed layout, fetched from `GET /users/{userId}/podcasts/{podcastId}/episodes`. Each episode row SHALL show the episode ID, generated date, day of week (short format), status badge, and action buttons. The episodes list SHALL be displayed under the "Episodes" tab, which is the default active tab. A "Publications" tab SHALL be displayed alongside it. The "Script" button label SHALL be used (not "View Script") and SHALL have equal fixed width as the "Publish" button. The Date column SHALL have a fixed width. All badge text SHALL be consistently lowercased. The podcast detail header SHALL display the podcast name with the style badge inline next to it, and a "Settings" button right-aligned. The podcast detail header SHALL display the cron schedule in human-readable form below the topic, prefixed with "Generates" (e.g., "Generates at 03:00 PM, Monday through Friday"), at the same text size as the topic description.

#### Scenario: Settings button in podcast header
- **WHEN** the podcast detail page loads
- **THEN** the header area displays the podcast name with the style badge inline next to it, and a "Settings" outline button right-aligned that navigates to `/podcasts/{podcastId}/settings`

#### Scenario: Cron schedule display
- **WHEN** the podcast detail page loads and the podcast has a cron schedule
- **THEN** the header area displays the cron expression converted to human-readable text below the topic, prefixed with "Generates" (e.g., "Generates at 03:00 PM, Monday through Friday"), at the same text size as the topic description, using the `cronstrue` library

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

#### Scenario: Publish button on unpublished GENERATED episodes
- **WHEN** an episode has status `GENERATED` and has not been published
- **THEN** a "Publish" button is displayed alongside the "Script" button

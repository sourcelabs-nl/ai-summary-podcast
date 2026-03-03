## MODIFIED Requirements

### Requirement: Episode list page
The system SHALL display a list of episodes for a podcast at `/podcasts/{podcastId}` within a tabbed layout, fetched from `GET /users/{userId}/podcasts/{podcastId}/episodes`. Each episode row SHALL be a clickable link that navigates to `/podcasts/{podcastId}/episodes/{episodeId}`. Each episode row SHALL show the episode ID, generated date, day of week (short format), status badge, and action buttons. The episodes list SHALL be displayed under the "Episodes" tab, which is the default active tab. A "Publications" tab SHALL be displayed alongside it. The Date column SHALL have a fixed width. All badge text SHALL be consistently lowercased. The podcast detail header SHALL display the podcast name with the style badge inline next to it, and a "Settings" button right-aligned. The podcast detail header SHALL display the cron schedule in human-readable form below the topic, prefixed with "Generates" (e.g., "Generates at 03:00 PM, Monday through Friday"), at the same text size as the topic description. The status filter SHALL be integrated into the Status column header as a dropdown menu, rather than as a standalone select component above the table.

#### Scenario: Click episode row navigates to detail page
- **WHEN** user clicks on an episode row
- **THEN** the app navigates to `/podcasts/{podcastId}/episodes/{episodeId}`

#### Scenario: Display episodes with columns
- **WHEN** the episode list page loads
- **THEN** episodes are displayed under the "Episodes" tab with columns: #, Date (fixed width), Day (short weekday name in muted text, e.g., "Mon"), Status (badge with optional Published badge), Script Model (compose model in `text-xs`, min-width column), TTS Model (`text-xs`, min-width column), Cost (right-aligned, combined LLM + TTS cost formatted as dollars, or em dash when unavailable), and Actions

#### Scenario: View button on episode rows
- **WHEN** an episode row is displayed
- **THEN** a "View" button with `outline` variant and a ChevronRight icon is displayed in the Actions column, navigating to the episode detail page

#### Scenario: Day column shows weekday
- **WHEN** episodes are displayed
- **THEN** each episode row shows the short weekday name (e.g., "Mon", "Tue") derived from the generated date

#### Scenario: Status badge for discarded episodes
- **WHEN** an episode has status `DISCARDED`
- **THEN** the status badge uses the `secondary` variant (muted grey) instead of the `default` variant

#### Scenario: Status badge for active statuses
- **WHEN** an episode has status `PENDING_REVIEW`, `APPROVED`, or `FAILED`
- **THEN** the status badge uses the `default` variant (orange)

#### Scenario: Status badge for generated episodes
- **WHEN** an episode has status `GENERATED`
- **THEN** the status badge uses the `outline` variant (white) to distinguish it from post-review statuses

#### Scenario: Published badge on episode row
- **WHEN** an episode has been published (has at least one publication with status PUBLISHED)
- **THEN** a "Published" badge with the `default` variant (orange) is displayed next to the status badge

#### Scenario: Filter episodes by status via header dropdown
- **WHEN** user clicks the Status column header and selects a status from the dropdown menu
- **THEN** only episodes matching that status are displayed (using `?status=` query param)

#### Scenario: Show all episodes via header dropdown
- **WHEN** user clicks the Status column header and selects "All statuses" from the dropdown menu
- **THEN** all episodes for the podcast are displayed

#### Scenario: Status header dropdown shows active filter
- **WHEN** the status filter dropdown is opened
- **THEN** the currently active filter option is shown with a check mark

#### Scenario: Status header indicates interactivity
- **WHEN** the episode list page loads
- **THEN** the Status column header displays a chevron icon indicating it is clickable for filtering

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
- **THEN** the header area displays the podcast name with the style badge inline next to it, and a "Settings" button (default variant, sm size, with Settings icon) right-aligned that navigates to `/podcasts/{podcastId}/settings`

#### Scenario: Podcast header layout order
- **WHEN** the podcast detail page loads
- **THEN** the header displays in this order: (1) podcast name + style badge inline, (2) cron schedule in `text-sm` italic (if available), (3) topic description in `text-sm` regular text

#### Scenario: Cron schedule display
- **WHEN** the podcast detail page loads and the podcast has a cron schedule
- **THEN** the header area displays the cron expression converted to human-readable text in italic below the podcast name, prefixed with "Generates" (e.g., "Generates at 03:00 PM, Monday through Friday"), using the `cronstrue` library

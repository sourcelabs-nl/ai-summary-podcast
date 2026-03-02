## MODIFIED Requirements

### Requirement: Episode list page
The system SHALL display a list of episodes for a podcast at `/podcasts/{podcastId}` within a tabbed layout, fetched from `GET /users/{userId}/podcasts/{podcastId}/episodes`. Each episode row SHALL show the episode ID, generated date, day of week (short format), status badge, and action buttons. The episodes list SHALL be displayed under the "Episodes" tab, which is the default active tab. A "Publications" tab SHALL be displayed alongside it. The "Script" button label SHALL be used (not "View Script") and SHALL have equal fixed width as the "Publish" button. The Date column SHALL have a fixed width. All badge text SHALL be consistently lowercased.

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

### Requirement: Badge text casing
All badges across the frontend SHALL render text in consistent lowercase using the `lowercase` CSS class applied to the badge base styles.

#### Scenario: Badge text is lowercased
- **WHEN** any badge is rendered (status badges, style badges, published badges)
- **THEN** the text is displayed in lowercase

### Requirement: Script viewer dialog
The system SHALL provide a "Script" button on each episode row that opens a wide shadcn Dialog (95vw, max 1600px) displaying the episode's `scriptText` in a chat-bubble style.

#### Scenario: View monologue script
- **WHEN** user opens the script dialog for an episode from a podcast with style `news-briefing`, `casual`, `deep-dive`, or `executive-summary`
- **THEN** each paragraph is rendered in a rounded card bubble with `bg-muted` background and `border-border`

#### Scenario: View dialogue script
- **WHEN** user opens the script dialog for an episode from a podcast with style `dialogue` or `interview`
- **THEN** the script is parsed for XML speaker tags and rendered as alternating chat bubbles — the first speaker's bubbles align left with `bg-muted` (grey), the second speaker's bubbles align right with `bg-primary` and `text-primary-foreground` (solid orange, white text), with speaker name labels above each bubble

#### Scenario: Fallback on parse failure
- **WHEN** a multi-speaker script fails to parse (malformed XML tags)
- **THEN** the script SHALL fall back to monologue bubble rendering

### Requirement: Application title
The frontend SHALL display "AI Podcast Studio" as the application name in the header and page metadata (title and description).

#### Scenario: Header displays application name
- **WHEN** any page is loaded
- **THEN** the header displays "AI Podcast Studio"

#### Scenario: Page metadata
- **WHEN** any page is loaded
- **THEN** the page title is "AI Podcast Studio" and description is "Dashboard for AI Podcast Studio"

### Requirement: Episode recap field
The `Episode` TypeScript interface SHALL include an optional `recap` field (string) for the episode summary.

#### Scenario: Recap available in type
- **WHEN** the frontend fetches episode data
- **THEN** the `recap` field is available on the Episode type for use in the publish wizard confirmation step

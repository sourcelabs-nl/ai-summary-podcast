## Purpose

Defines the frontend page for previewing upcoming episode content and dry-run script preview.

## Requirements

### Requirement: Upcoming content page with tabbed layout
The system SHALL provide a page at `/podcasts/[podcastId]/upcoming` that uses a tabbed layout with Articles and Script tabs, matching the episode detail page pattern.

#### Scenario: Page header
- **WHEN** user navigates to the upcoming content page
- **THEN** the page displays a back link "Back to Episodes", a header "Upcoming Episode" with article count, source count, and next scheduled generation time (parsed from the podcast's cron expression), and a "Generate Episode" button

#### Scenario: Next generation schedule
- **WHEN** the podcast has a cron expression configured
- **THEN** the header subtitle shows the next generation date and time (e.g., "Will be generated Wed, Mar 4 at 06:00 AM")

#### Scenario: No articles
- **WHEN** the page loads and there are no upcoming articles
- **THEN** the Articles tab displays a message indicating no content has been collected yet

### Requirement: Articles tab
The Articles tab (default) SHALL display all articles collected for the next episode, grouped by source with collapsible sections.

#### Scenario: Articles displayed
- **WHEN** the Articles tab is active and there are upcoming articles
- **THEN** articles are fetched from `GET /api/users/{userId}/podcasts/{podcastId}/upcoming-articles` and displayed using the same grouped-by-source pattern as the episode detail Articles tab

### Requirement: Script tab with inline preview
The Script tab SHALL allow the user to generate a dry-run script preview inline via SSE, displaying real-time progress stages, word count, and estimated audio duration.

#### Scenario: No preview generated yet
- **WHEN** the Script tab is selected and no preview has been generated
- **THEN** a centered "Preview Script" button is displayed with a message "No script preview generated yet."

#### Scenario: Preview Script clicked
- **WHEN** user clicks "Preview Script"
- **THEN** a GET SSE connection is opened to `/api/users/{userId}/podcasts/{podcastId}/preview`, a loading state is shown with the current pipeline stage (e.g., "Scoring 5 articles...", "Composing script..."), and on receiving the `result` event the script is rendered inline using the ScriptContent component with word count and estimated duration (~150 words/minute) shown above the script

#### Scenario: Script tab label with word count
- **WHEN** a preview has been generated
- **THEN** the Script tab label shows the word count (e.g., "Script (2,450 words)")

#### Scenario: Preview with no content
- **WHEN** user clicks "Preview Script" but the `result` event contains a message instead of scriptText
- **THEN** the message is displayed to the user as an error banner

#### Scenario: Preview connection error
- **WHEN** the SSE connection fails or emits an error event
- **THEN** the loading state is cleared and an error message is displayed

### Requirement: Generate Episode action
The page header SHALL display a "Generate Episode" button visible from any tab.

#### Scenario: Generate Episode clicked
- **WHEN** user clicks "Generate Episode"
- **THEN** a POST request is made to `/api/users/{userId}/podcasts/{podcastId}/generate`, and on success the user is navigated to the new episode's detail page at `/podcasts/{podcastId}/episodes/{episodeId}`

#### Scenario: Generate with no content
- **WHEN** user clicks "Generate Episode" but there are no relevant articles
- **THEN** the response message is displayed to the user as an error banner

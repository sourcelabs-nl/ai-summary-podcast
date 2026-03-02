## ADDED Requirements

### Requirement: User picker
The system SHALL display a user picker dropdown in the header that fetches all users from `GET /users` and allows selecting one. The selected user context SHALL be used for all subsequent API calls. The dropdown popover SHALL align to the end (right) of the trigger.

#### Scenario: User selection persists across navigation
- **WHEN** user selects a user from the picker and navigates between pages
- **THEN** the selected user remains active and all API calls use that user's ID

#### Scenario: No users available
- **WHEN** the user picker fetches from `GET /users` and receives an empty list
- **THEN** the picker SHALL display a message indicating no users are available

### Requirement: Podcast overview page
The system SHALL display a vertical list of all podcasts for the selected user at the `/podcasts` route, fetched from `GET /users/{userId}/podcasts`. Each podcast row SHALL show the podcast name, an orange style badge (default variant), and topic aligned to the right.

#### Scenario: Display podcasts
- **WHEN** a user is selected and the podcasts page loads
- **THEN** all podcasts for that user are displayed in a single-column list with name, orange style badge, and topic

#### Scenario: Navigate to episode list
- **WHEN** user clicks on a podcast row
- **THEN** the app navigates to `/podcasts/{podcastId}` showing that podcast's episodes

#### Scenario: No podcasts
- **WHEN** the selected user has no podcasts
- **THEN** an empty state message is displayed

### Requirement: Episode list page
The system SHALL display a list of episodes for a podcast at `/podcasts/{podcastId}`, fetched from `GET /users/{userId}/podcasts/{podcastId}/episodes`. Each episode row SHALL show the episode ID, generated date, and an orange status badge (all statuses use the default/primary variant for consistent styling).

#### Scenario: Display episodes with status badges
- **WHEN** the episode list page loads
- **THEN** episodes are displayed with ID, formatted date, and an orange status badge showing the status text (PENDING_REVIEW, APPROVED, GENERATED, FAILED, DISCARDED)

#### Scenario: Filter episodes by status
- **WHEN** user selects a status from the filter dropdown
- **THEN** only episodes matching that status are displayed (using `?status=` query param)

#### Scenario: Show all episodes
- **WHEN** user clears the status filter
- **THEN** all episodes for the podcast are displayed

### Requirement: Approve episode
The system SHALL display an "Approve" button on episodes with status `PENDING_REVIEW`. Clicking the button SHALL call `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/approve`.

#### Scenario: Successful approval
- **WHEN** user clicks "Approve" on a PENDING_REVIEW episode
- **THEN** the API is called, the episode status updates to APPROVED, and the button is removed

#### Scenario: Approve button visibility
- **WHEN** an episode has a status other than PENDING_REVIEW
- **THEN** the "Approve" button SHALL NOT be displayed

### Requirement: Discard episode
The system SHALL display a "Discard" button on episodes with status `PENDING_REVIEW`. Clicking the button SHALL call `POST /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}/discard`.

#### Scenario: Successful discard
- **WHEN** user clicks "Discard" on a PENDING_REVIEW episode
- **THEN** the API is called and the episode status updates to DISCARDED

#### Scenario: Discard button visibility
- **WHEN** an episode has a status other than PENDING_REVIEW
- **THEN** the "Discard" button SHALL NOT be displayed

### Requirement: Script viewer dialog
The system SHALL provide a "View Script" button on each episode row that opens a wide shadcn Dialog (90vw, max 5xl) displaying the episode's `scriptText` in a chat-bubble style.

#### Scenario: View monologue script
- **WHEN** user opens the script dialog for an episode from a podcast with style `news-briefing`, `casual`, `deep-dive`, or `executive-summary`
- **THEN** each paragraph is rendered in a rounded card bubble with a subtle primary-colored border and background

#### Scenario: View dialogue script
- **WHEN** user opens the script dialog for an episode from a podcast with style `dialogue` or `interview`
- **THEN** the script is parsed for XML speaker tags and rendered as alternating chat bubbles — the first speaker's bubbles align left (primary color), the second speaker's bubbles align right (emerald color), with speaker name labels above each bubble

#### Scenario: Fallback on parse failure
- **WHEN** a multi-speaker script fails to parse (malformed XML tags)
- **THEN** the script SHALL fall back to monologue bubble rendering

### Requirement: Orange theme
The frontend SHALL use the shadcn/ui orange color theme with oklch color variables following the official shadcn theming documentation. All badges, buttons (default variant), and focus rings SHALL use the primary orange color for consistent branding.

#### Scenario: Consistent orange branding
- **WHEN** any component uses the `default` variant (Badge, Button)
- **THEN** it renders with the primary orange color (`oklch(0.705 0.187 47.604)` in light mode)

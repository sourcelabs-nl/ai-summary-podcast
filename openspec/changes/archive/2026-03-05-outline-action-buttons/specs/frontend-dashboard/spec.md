## MODIFIED Requirements

### Requirement: Podcast overview page
The system SHALL display a vertical list of all podcasts for the selected user at the `/podcasts` route, fetched from `GET /users/{userId}/podcasts`. Each podcast row SHALL show the podcast name, an orange style badge (default variant), and topic aligned to the right. Each podcast card SHALL also display a gear icon-only button (Settings) using `size="icon-lg"` that navigates to `/podcasts/{podcastId}/settings` without triggering navigation to the podcast detail page. The button SHALL include a `title` attribute with text "Settings".

#### Scenario: Display podcasts
- **WHEN** a user is selected and the podcasts page loads
- **THEN** all podcasts for that user are displayed in a single-column list with name, orange style badge, topic, and an icon-only Settings button with hover tooltip

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
The system SHALL display a list of episodes for a podcast at `/podcasts/{podcastId}` within a tabbed layout, fetched from `GET /users/{userId}/podcasts/{podcastId}/episodes`. Each episode row SHALL be a clickable link that navigates to `/podcasts/{podcastId}/episodes/{episodeId}`. Each episode row SHALL show the episode ID, generated date, day of week (short format), status badge, and action buttons. The episodes list SHALL be displayed under the "Episodes" tab, which is the default active tab. A "Publications" tab SHALL be displayed alongside it. The Date column SHALL have a fixed width. All badge text SHALL be consistently lowercased. The podcast detail header SHALL display the podcast name with the style badge inline next to it, and an icon-only "Settings" button right-aligned. The podcast detail header SHALL display the topic and cron schedule combined on one line in `text-sm` format, with the cron schedule in human-readable form separated by a dot separator (e.g., `{topic} · at 03:00 PM, Monday through Friday`). The status filter SHALL be integrated into the Status column header as a dropdown menu, rather than as a standalone select component above the table. All action buttons (Settings, Details, Publish, Approve) SHALL be icon-only using `size="icon-lg"` with `title` attributes for hover tooltips. Destructive buttons (Discard) SHALL keep the `destructive` variant, be icon-only, and include a `title` attribute.

#### Scenario: Action buttons are icon-only with tooltips
- **WHEN** action buttons are rendered on episode rows or the podcast header
- **THEN** all buttons are icon-only (no text labels) with `title` attributes providing hover alt text

#### Scenario: Settings button in podcast header
- **WHEN** the podcast detail page loads
- **THEN** the header area displays the podcast name with the style badge inline next to it, and an icon-only Settings button (icon-lg size, with Settings icon and title="Settings") right-aligned that navigates to `/podcasts/{podcastId}/settings`

### Requirement: User picker
The system SHALL display a user picker dropdown in the header that fetches all users from `GET /users` and allows selecting one. The selected user context SHALL be used for all subsequent API calls. The dropdown popover SHALL align to the end (right) of the trigger. A gear icon button (Settings icon from lucide-react) SHALL be displayed to the right of the user dropdown, navigating to `/settings`. The icon button SHALL use the `ghost` variant with `text-primary-foreground` styling and SHALL have a `border border-input rounded-md h-9` style matching the user dropdown trigger's border appearance. The icon button SHALL include a `title` attribute with text "User settings".

#### Scenario: User selection persists across navigation
- **WHEN** user selects a user from the picker and navigates between pages
- **THEN** the selected user remains active and all API calls use that user's ID

#### Scenario: No users available
- **WHEN** the user picker fetches from `GET /users` and receives an empty list
- **THEN** the picker SHALL display a message indicating no users are available

#### Scenario: Settings icon navigates to preferences
- **WHEN** the user clicks the gear icon button next to the user dropdown
- **THEN** the app navigates to `/settings`

#### Scenario: Settings icon hidden when no user selected
- **WHEN** no user is selected (loading or no users available)
- **THEN** the gear icon button SHALL NOT be displayed

#### Scenario: Settings icon has tooltip
- **WHEN** the user hovers over the gear icon button
- **THEN** a tooltip with text "User settings" is displayed

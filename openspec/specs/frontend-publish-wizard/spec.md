## Purpose

Defines the requirements for the frontend publish wizard, publications tab, and episode publishing UI on the Next.js dashboard.

## Requirements

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
- **THEN** the wizard shows an error message indicating the episode is already published to this target

#### Scenario: Close wizard refreshes data
- **WHEN** user closes the wizard dialog (via "Done" button or clicking outside)
- **THEN** the episodes list and publications data SHALL be refreshed

### Requirement: Publish button on episodes table
The system SHALL display a "Publish" button on each episode row that has status `GENERATED` and has NOT already been published. The system SHALL fetch publication status for all episodes on page load to determine which episodes are already published. The "Publish" and "Script" buttons SHALL have equal fixed width (`w-20`).

#### Scenario: Publish button visible for unpublished GENERATED episodes
- **WHEN** an episode has status `GENERATED` and no publication with status `PUBLISHED` exists
- **THEN** a "Publish" button is displayed in the actions column

#### Scenario: Publish button hidden for already published episodes
- **WHEN** an episode has status `GENERATED` but already has a publication with status `PUBLISHED`
- **THEN** the "Publish" button SHALL NOT be displayed

#### Scenario: Publish button hidden for non-GENERATED episodes
- **WHEN** an episode has status `PENDING_REVIEW`, `APPROVED`, `FAILED`, or `DISCARDED`
- **THEN** the "Publish" button SHALL NOT be displayed

### Requirement: Published badge on episodes table
The system SHALL display a "Published" badge (outline variant) next to the status badge for episodes that have been published to any target.

#### Scenario: Published badge displayed
- **WHEN** an episode has a publication with status `PUBLISHED`
- **THEN** an outline badge with text "Published" is displayed next to the status badge

#### Scenario: Published badge not displayed
- **WHEN** an episode has no publications or only non-PUBLISHED publications
- **THEN** no "Published" badge is displayed

### Requirement: Publications tab
The system SHALL display a "Publications" tab on the podcast detail page alongside the existing "Episodes" tab. The tab SHALL show a table of all publications for the podcast's episodes with columns: #, Published, Status, Target, URL, Actions. The target column SHALL display a cloud icon and the properly-cased target name (e.g., "SoundCloud"). The URL column SHALL display "Track" and "Playlist" links for SoundCloud publications, where "Track" links to the individual track and "Playlist" links to the user's sets page (derived from the track URL).

#### Scenario: Display publications table
- **WHEN** user clicks the "Publications" tab
- **THEN** a table is displayed with columns: # (episode number), Published (date), Status (badge), Target (cloud icon + display name), URL ("Track | Playlist" links), Actions (Republish button)

#### Scenario: SoundCloud URL links
- **WHEN** a SoundCloud publication has an external URL
- **THEN** the URL column displays "Track" linking to the track URL and "Playlist" linking to the user's SoundCloud sets page, separated by a pipe character

#### Scenario: Target display name
- **WHEN** a publication has target "soundcloud"
- **THEN** the target column displays a cloud icon followed by "SoundCloud"

#### Scenario: No publications
- **WHEN** the podcast has no published episodes
- **THEN** the publications tab shows an empty state message "No publications found."

#### Scenario: Publication status badges
- **WHEN** publications are displayed
- **THEN** status badges use the `default` variant (orange) for all statuses (PENDING, PUBLISHED, FAILED)

### Requirement: Republish from publications tab
The system SHALL display a "Republish" button on each publication row in the publications tab. Clicking the button SHALL show a confirmation dialog before calling the publish API.

#### Scenario: Republish confirmation dialog
- **WHEN** user clicks the "Republish" button on a publication row
- **THEN** a dialog appears asking "Are you sure you want to republish episode #N to {target}?" with Cancel and Republish buttons

#### Scenario: Republish confirmed
- **WHEN** user clicks "Republish" in the confirmation dialog
- **THEN** the system calls `POST .../episodes/{id}/publish/{target}` and refreshes the publications and episodes data on completion

#### Scenario: Republish cancelled
- **WHEN** user clicks "Cancel" in the confirmation dialog
- **THEN** the dialog closes and no API call is made

### Requirement: EpisodePublication TypeScript type
The system SHALL define an `EpisodePublication` interface in `frontend/src/lib/types.ts` with fields: `id` (number), `episodeId` (number), `target` (string), `status` (string), `externalId` (string | null), `externalUrl` (string | null), `errorMessage` (string | null), `publishedAt` (string | null), `createdAt` (string).

#### Scenario: Type used for API responses
- **WHEN** the frontend fetches publication data from `GET .../publications`
- **THEN** the response SHALL be typed as `EpisodePublication[]`

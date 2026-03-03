## ADDED Requirements

### Requirement: Source TypeScript interface
The frontend SHALL define a `Source` interface in `types.ts` with fields: `id` (string), `podcastId` (string), `type` (string), `url` (string), `pollIntervalMinutes` (number), `enabled` (boolean), `label` (string | null), `createdAt` (string).

#### Scenario: Source type available for use
- **WHEN** the frontend fetches source data from the API
- **THEN** the response can be typed with the `Source` interface

### Requirement: Sources tab on podcast detail page
The podcast detail page SHALL display a "Sources" tab alongside the existing "Episodes" and "Publications" tabs. The tab SHALL render a `SourcesTab` component.

#### Scenario: Sources tab visible
- **WHEN** the podcast detail page loads
- **THEN** three tabs are displayed: "Episodes" (default), "Publications", and "Sources"

#### Scenario: Sources tab shows source list
- **WHEN** user clicks the "Sources" tab
- **THEN** the `SourcesTab` component loads and displays sources for the podcast

### Requirement: Source list table
The `SourcesTab` component SHALL fetch sources from `GET /api/users/{userId}/podcasts/{podcastId}/sources` and display them in a table with columns: Label (display label, falling back to URL if null), Type (source type badge), Poll Interval (formatted as minutes), Enabled (visual indicator), and Actions (edit and delete buttons).

#### Scenario: Display sources
- **WHEN** the Sources tab loads and the podcast has sources
- **THEN** all sources are displayed in a table with the specified columns

#### Scenario: No sources
- **WHEN** the Sources tab loads and the podcast has no sources
- **THEN** an empty state message is displayed

#### Scenario: Source label fallback
- **WHEN** a source has a null label
- **THEN** the URL is displayed in the Label column instead

### Requirement: Add source dialog
The Sources tab SHALL display an "Add Source" button that opens a dialog. The dialog SHALL contain a form with fields: type (select with options: rss, website, twitter, youtube), url (text input), label (text input, optional), poll interval in minutes (number input, default 30), and enabled (switch, default true). Submitting the form SHALL call `POST /api/users/{userId}/podcasts/{podcastId}/sources` and refresh the source list.

#### Scenario: Add a new source
- **WHEN** user clicks "Add Source", fills in the form, and submits
- **THEN** the source is created via the API, the dialog closes, and the source list refreshes

#### Scenario: Cancel add
- **WHEN** user opens the Add Source dialog and clicks cancel
- **THEN** the dialog closes without creating a source

### Requirement: Edit source dialog
Each source row SHALL have an edit button that opens a dialog pre-populated with the source's current values. Submitting the form SHALL call `PUT /api/users/{userId}/podcasts/{podcastId}/sources/{sourceId}` and refresh the source list.

#### Scenario: Edit an existing source
- **WHEN** user clicks edit on a source row, modifies fields, and submits
- **THEN** the source is updated via the API, the dialog closes, and the source list refreshes

#### Scenario: Cancel edit
- **WHEN** user opens the Edit dialog and clicks cancel
- **THEN** the dialog closes without updating the source

### Requirement: Delete source with confirmation
Each source row SHALL have a delete button. Clicking it SHALL open an AlertDialog warning that deleting the source will also delete all associated articles. Confirming SHALL call `DELETE /api/users/{userId}/podcasts/{podcastId}/sources/{sourceId}` and refresh the source list.

#### Scenario: Delete a source
- **WHEN** user clicks delete, confirms in the AlertDialog
- **THEN** the source is deleted via the API and the source list refreshes

#### Scenario: Cancel delete
- **WHEN** user clicks delete and then cancels the AlertDialog
- **THEN** the source is not deleted

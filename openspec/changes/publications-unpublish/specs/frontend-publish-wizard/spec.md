## MODIFIED Requirements

### Requirement: Publications tab
The system SHALL display a "Publications" tab on the podcast detail page alongside the existing "Episodes" tab. The tab SHALL show a table of all publications for the podcast's episodes with columns: #, Date, Day, Published, Status, Target, URL, Actions. The Date column SHALL display the episode's generated date. The Day column SHALL display the day of the week. The target column SHALL display a cloud icon and the properly-cased target name (e.g., "SoundCloud"). The URL column SHALL display "Track" and "Playlist" links for SoundCloud publications, where "Track" links to the individual track and "Playlist" links to the user's sets page. The Actions column SHALL display a Republish button and an Unpublish button for PUBLISHED records.

#### Scenario: Display publications table with episode date
- **WHEN** user clicks the "Publications" tab
- **THEN** a table is displayed with columns: # (episode number), Date (episode date), Day (day of week), Published (publication date), Status (badge), Target (icon + name), URL (links), Actions (Republish + Unpublish buttons)

#### Scenario: Unpublish button visible for PUBLISHED records
- **WHEN** a publication has status `PUBLISHED`
- **THEN** an Unpublish button (destructive variant, icon-only) is displayed in the actions column

#### Scenario: Unpublish button hidden for non-PUBLISHED records
- **WHEN** a publication has status `UNPUBLISHED`, `FAILED`, or `PENDING`
- **THEN** the Unpublish button is NOT displayed

#### Scenario: Unpublish confirmation dialog
- **WHEN** user clicks the Unpublish button on a publication row
- **THEN** a dialog appears asking "Are you sure you want to unpublish episode #N from {target}?" with Cancel and Unpublish buttons

#### Scenario: Unpublish confirmed
- **WHEN** user clicks "Unpublish" in the confirmation dialog
- **THEN** the system calls `DELETE .../episodes/{id}/publications/{target}` and refreshes the publications and episodes data on completion

#### Scenario: UNPUBLISHED status badge
- **WHEN** a publication has status `UNPUBLISHED`
- **THEN** the status badge uses the `secondary` variant (grey)

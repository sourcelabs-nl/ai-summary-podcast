## MODIFIED Requirements

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

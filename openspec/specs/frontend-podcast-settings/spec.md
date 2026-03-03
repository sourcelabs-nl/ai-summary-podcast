## Purpose

Defines the requirements for the podcast settings page in the Next.js frontend dashboard, enabling users to view and edit all podcast configuration through a tabbed form interface.

## Requirements

### Requirement: Settings page route
The system SHALL provide a settings page at `/podcasts/[podcastId]/settings` that fetches the podcast from `GET /users/{userId}/podcasts/{podcastId}` and displays all configuration fields in an editable form.

#### Scenario: Load settings page
- **WHEN** user navigates to `/podcasts/{podcastId}/settings`
- **THEN** the page fetches the podcast data and displays a "Back to podcast" link, the page title "Settings: {podcastName}", and a tabbed settings form

#### Scenario: Podcast not found
- **WHEN** the podcast ID does not exist or does not belong to the selected user
- **THEN** the page displays "Podcast not found."

### Requirement: Settings sub-tabs
The settings form SHALL be organized into five sub-tabs: General, LLM, TTS, Content, and Integrations. The General tab SHALL be the default active tab.

#### Scenario: General tab fields
- **WHEN** the General tab is active
- **THEN** the form displays editable fields for: name (text), topic (text), language (text), style (select), cron (text), customInstructions (textarea), requireReview (checkbox)

#### Scenario: LLM tab fields
- **WHEN** the LLM tab is active
- **THEN** the form displays editable fields for: llmModels (key-value editor), relevanceThreshold (number), maxLlmCostCents (number), fullBodyThreshold (number), maxArticleAgeDays (number), targetWords (number)

#### Scenario: TTS tab fields
- **WHEN** the TTS tab is active
- **THEN** the form displays editable fields for: ttsProvider (select), ttsVoices (key-value editor), ttsSettings (key-value editor), speakerNames (key-value editor)

#### Scenario: Content tab fields
- **WHEN** the Content tab is active
- **THEN** the form displays editable fields for: sponsor (key-value editor), pronunciations (key-value editor)

#### Scenario: Integrations tab fields
- **WHEN** the Integrations tab is active
- **THEN** the form displays editable fields for: soundcloudPlaylistId (text)

### Requirement: Key-value editor
JSON map fields SHALL be edited using a structured key-value row editor. Each row SHALL display a text input for the key, a text input for the value, and a remove button. An "Add row" button SHALL allow adding new entries.

#### Scenario: Display existing entries
- **WHEN** a JSON map field has existing data (e.g., `{"filter": "haiku", "compose": "sonnet"}`)
- **THEN** each entry is rendered as a row with key and value pre-filled in text inputs

#### Scenario: Add new entry
- **WHEN** user clicks "Add row" on a key-value editor
- **THEN** a new empty row is appended with blank key and value inputs

#### Scenario: Remove entry
- **WHEN** user clicks the remove button on a key-value row
- **THEN** that row is removed from the editor

#### Scenario: Empty map
- **WHEN** a JSON map field is null or empty
- **THEN** the editor displays no rows, only the "Add row" button

### Requirement: Save settings
The system SHALL display a single Save button at the bottom of the settings page. Clicking Save SHALL send a `PUT /users/{userId}/podcasts/{podcastId}` request with all current form values.

#### Scenario: Successful save
- **WHEN** user clicks Save and the API returns 200
- **THEN** the form updates with the response data and a success indication is shown

#### Scenario: Save with API error
- **WHEN** user clicks Save and the API returns an error
- **THEN** the error message from the API response is displayed to the user

#### Scenario: Key-value serialization
- **WHEN** the form is saved and key-value editors contain rows
- **THEN** each key-value editor's rows are serialized into a JSON object (e.g., rows `[{key: "a", value: "b"}]` become `{"a": "b"}`). Empty keys are excluded. If no rows remain, the field is sent as null.

### Requirement: System default placeholders on nullable fields
Nullable number fields in the settings form SHALL display placeholder text showing the system default value when the field is empty. The placeholder values SHALL be fetched from the `GET /config/defaults` API endpoint. The placeholder format SHALL be `{value} (system default)`.

#### Scenario: Empty target words shows default
- **WHEN** the LLM tab is active and targetWords is null/empty
- **THEN** the input displays placeholder text with the targetWords value from the defaults endpoint followed by "(system default)"

#### Scenario: Empty max LLM cost shows default
- **WHEN** the LLM tab is active and maxLlmCostCents is null/empty
- **THEN** the input displays placeholder text with the maxLlmCostCents value from the defaults endpoint followed by "(system default)"

#### Scenario: Empty full body threshold shows default
- **WHEN** the LLM tab is active and fullBodyThreshold is null/empty
- **THEN** the input displays placeholder text with the fullBodyThreshold value from the defaults endpoint followed by "(system default)"

#### Scenario: Empty max article age shows default
- **WHEN** the LLM tab is active and maxArticleAgeDays is null/empty
- **THEN** the input displays placeholder text with the maxArticleAgeDays value from the defaults endpoint followed by "(system default)"

#### Scenario: Explicit value hides placeholder
- **WHEN** a nullable field has an explicit user-set value
- **THEN** the placeholder is not visible and the user's value is displayed

### Requirement: LLM models default hint
The LLM Models key-value editor SHALL display a helper text below it showing the system default model assignments fetched from the `GET /config/defaults` endpoint.

#### Scenario: Default models displayed
- **WHEN** the LLM tab is active and defaults have been loaded
- **THEN** a helper text is displayed below the editor listing default model assignments (e.g., "System defaults: filter = openai/gpt-4o-mini, compose = anthropic/claude-sonnet-4.6")

#### Scenario: Defaults not loaded
- **WHEN** the defaults endpoint fails or has not loaded yet
- **THEN** no helper text or placeholder is displayed

### Requirement: Podcast type completeness
The `Podcast` TypeScript interface SHALL include all fields returned by the `GET /users/{userId}/podcasts/{podcastId}` API endpoint: id, userId, name, topic, language, llmModels, ttsProvider, ttsVoices, ttsSettings, style, targetWords, cron, customInstructions, relevanceThreshold, requireReview, maxLlmCostCents, maxArticleAgeDays, speakerNames, fullBodyThreshold, sponsor, pronunciations, lastGeneratedAt.

#### Scenario: All API fields available in type
- **WHEN** the frontend fetches podcast data
- **THEN** all fields from the API response are typed and accessible on the `Podcast` interface

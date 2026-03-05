## MODIFIED Requirements

### Requirement: Provider config list
The API Keys section SHALL display a unified list of all configured providers for the selected user, fetched from `GET /users/{userId}/api-keys`. Each row SHALL show the provider name, category badge (LLM or TTS), base URL, and icon-only action buttons (Edit, Remove). The category badge SHALL use the `default` variant for LLM and `outline` variant for TTS to visually distinguish them. The Edit button SHALL include a `title` attribute "Edit provider". The Remove button SHALL include a `title` attribute "Remove provider".

#### Scenario: Display configured providers
- **WHEN** the settings page loads and the user has configured providers
- **THEN** all providers are displayed in a single list with provider name, category badge, base URL, and icon-only action buttons with hover tooltips

#### Scenario: No providers configured
- **WHEN** the settings page loads and the user has no configured providers
- **THEN** an empty state message is displayed (e.g., "No API keys configured yet")

#### Scenario: Remove provider config
- **WHEN** the user clicks Remove on a provider row
- **THEN** the system calls `DELETE /users/{userId}/api-keys/{category}/{provider}` and removes the row from the list

### Requirement: Add provider dialog
The settings page SHALL provide an icon-only "Add Provider" button using `size="icon-lg"` (Plus icon) that opens a wizard-style dialog. The button SHALL include a `title` attribute with text "Add provider". The dialog SHALL have the following fields in order: (1) Category selector (LLM or TTS), (2) Provider dropdown filtered by selected category (LLM: openrouter, openai, ollama; TTS: openai, elevenlabs, inworld), (3) API Key input field (password type), (4) Base URL field pre-filled with the provider's known default URL and editable for override. Submitting SHALL call `PUT /users/{userId}/api-keys/{category}` with provider, apiKey, and optional baseUrl. Providers already configured for the selected category SHALL be excluded from the dropdown to prevent duplicates.

#### Scenario: Add provider button has tooltip
- **WHEN** the user hovers over the Add Provider button
- **THEN** a tooltip with text "Add provider" is displayed

#### Scenario: Add new provider
- **WHEN** the user opens the Add Provider dialog, selects a category and provider, enters an API key, and submits
- **THEN** the system calls `PUT /users/{userId}/api-keys/{category}` and adds the new provider to the list

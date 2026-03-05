# Capability: Frontend User Preferences

## Purpose

Frontend settings page for user profile editing and API key management, accessible via gear icon in the header.

## Requirements

### Requirement: Settings page route
The frontend SHALL provide a settings page at the `/settings` route. The page SHALL display two sections: Profile and API Keys.

#### Scenario: Navigate to settings
- **WHEN** the user clicks the gear icon in the header
- **THEN** the app navigates to `/settings` and displays the settings page with Profile and API Keys sections

### Requirement: Profile section
The settings page SHALL display a Profile section with an editable name field and a Save button. The name field SHALL be pre-filled with the current user's name from the user context. Saving SHALL call `PUT /users/{userId}` with the updated name. After a successful save, the user context SHALL be refreshed to reflect the updated name.

#### Scenario: Edit user name
- **WHEN** the user changes the name field and clicks Save
- **THEN** the system calls `PUT /users/{userId}` with the new name and updates the user context

#### Scenario: Name field pre-filled
- **WHEN** the settings page loads
- **THEN** the name field displays the currently selected user's name

### Requirement: API Keys section with security notice
The settings page SHALL display an API Keys section with a security notice: "All API keys are stored encrypted. Decryption requires the application master key." The notice SHALL use a lock icon and muted styling.

#### Scenario: Security notice displayed
- **WHEN** the settings page loads
- **THEN** the API Keys section displays the encryption security notice with a lock icon

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
The settings page SHALL provide an icon-only "Add Provider" button using `size="icon-lg"` (Plus icon) with a `title` attribute "Add provider" that opens a wizard-style dialog. The dialog SHALL have the following fields in order: (1) Category selector (LLM or TTS), (2) Provider dropdown filtered by selected category (LLM: openrouter, openai, ollama; TTS: openai, elevenlabs, inworld), (3) API Key input field (password type), (4) Base URL field pre-filled with the provider's known default URL and editable for override. Submitting SHALL call `PUT /users/{userId}/api-keys/{category}` with provider, apiKey, and optional baseUrl. Providers already configured for the selected category SHALL be excluded from the dropdown to prevent duplicates.

#### Scenario: Add new provider
- **WHEN** the user opens the Add Provider dialog, selects a category and provider, enters an API key, and submits
- **THEN** the system calls `PUT /users/{userId}/api-keys/{category}` and adds the new provider to the list

#### Scenario: Provider dropdown filtered by category
- **WHEN** the user selects LLM as the category
- **THEN** the provider dropdown shows only openrouter, openai, and ollama

#### Scenario: Provider dropdown filtered for TTS
- **WHEN** the user selects TTS as the category
- **THEN** the provider dropdown shows only openai, elevenlabs, and inworld

#### Scenario: Base URL pre-filled
- **WHEN** the user selects a known provider
- **THEN** the Base URL field is pre-filled with the provider's default URL

#### Scenario: Already-configured providers excluded
- **WHEN** the user already has openrouter configured for LLM and opens the Add Provider dialog with LLM selected
- **THEN** openrouter is not shown in the provider dropdown

#### Scenario: API key optional for ollama
- **WHEN** the user selects ollama as the provider
- **THEN** the API Key field is not required (ollama does not need authentication)

### Requirement: Edit provider dialog
The settings page SHALL provide an Edit button on each provider row that opens the wizard-style dialog in edit mode. In edit mode, the category and provider fields SHALL be read-only (displayed but not editable). The API Key field SHALL be empty (the existing key is never shown). The Base URL field SHALL show the current base URL. Submitting SHALL call `PUT /users/{userId}/api-keys/{category}` to update the config.

#### Scenario: Edit existing provider
- **WHEN** the user clicks Edit on a provider row
- **THEN** the dialog opens with category and provider read-only, API key empty, and base URL pre-filled with the current value

#### Scenario: Update only base URL
- **WHEN** the user edits a provider, changes only the base URL, and leaves the API key empty
- **THEN** the system calls `PUT /users/{userId}/api-keys/{category}` with the new base URL and null apiKey (preserving the existing key)

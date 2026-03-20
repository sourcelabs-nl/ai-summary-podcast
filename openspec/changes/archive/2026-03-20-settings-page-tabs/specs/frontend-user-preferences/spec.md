## MODIFIED Requirements

### Requirement: Settings page layout
The settings page at `/settings` SHALL display all user configuration in a single tabbed interface with three tabs: Profile, API Keys, Publishing. The active tab SHALL be persisted in the URL query parameter `tab` (defaulting to `profile`).

#### Scenario: Default tab
- **WHEN** user navigates to `/settings` without a tab parameter
- **THEN** the Profile tab is active

#### Scenario: Tab from URL
- **WHEN** user navigates to `/settings?tab=publishing`
- **THEN** the Publishing tab is active

#### Scenario: Profile tab content
- **WHEN** the Profile tab is active
- **THEN** the page displays a name input field with a Save button

#### Scenario: API Keys tab content
- **WHEN** the API Keys tab is active
- **THEN** the page displays the provider configuration table with Add/Edit/Remove actions and the encrypted storage notice

#### Scenario: Publishing tab content
- **WHEN** the Publishing tab is active
- **THEN** the page displays FTP credentials form (host, port, username, password, TLS toggle) with Save and Test buttons, and SoundCloud credentials form (client ID, client secret, callback URI) with Save and Test buttons

### Requirement: Toast notifications for settings
All save and test feedback on the settings page SHALL use sonner toast notifications instead of inline messages.

#### Scenario: Successful save
- **WHEN** a save operation succeeds (profile name, FTP credentials, SoundCloud credentials)
- **THEN** a success toast is shown

#### Scenario: Failed save
- **WHEN** a save operation fails
- **THEN** an error toast is shown with the error message

#### Scenario: Connection test result
- **WHEN** an FTP or SoundCloud connection test completes
- **THEN** a success or error toast is shown with the result

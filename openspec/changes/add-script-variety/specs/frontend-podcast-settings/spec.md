## ADDED Requirements

### Requirement: Settings UI exposes composeTemperature

The podcast settings page SHALL include a numeric input for `composeTemperature` constrained to `[0.0, 2.0]` with step `0.05`. The input SHALL show the system default (`0.95`) as a placeholder when the value is unset. Saving SHALL round-trip the value through `PUT /users/{userId}/podcasts/{podcastId}`.

#### Scenario: User edits temperature

- **WHEN** the user changes the compose temperature field to `0.7` and clicks Save
- **THEN** the PUT request body includes `composeTemperature=0.7` and the value persists on reload

#### Scenario: Unset temperature shows placeholder

- **WHEN** the settings page loads for a podcast with no `composeTemperature` override
- **THEN** the input renders empty with placeholder text indicating the default `0.95`

## ADDED Requirements

### Requirement: composeTemperature podcast field

A podcast SHALL accept an optional `composeTemperature` field (decimal in `[0.0, 2.0]`). When unset, the compose stage applies the system default of `0.95`. The field SHALL be persisted on the `podcasts` table, returned by the podcast GET endpoint, and accepted by the create/update endpoints.

#### Scenario: Field round-trips through the API

- **WHEN** a client sends `PUT /users/{userId}/podcasts/{podcastId}` with `composeTemperature=0.7`
- **THEN** the value is persisted and the next `GET` for the same podcast returns `composeTemperature=0.7`

#### Scenario: Out-of-range value rejected

- **WHEN** a client sends `composeTemperature=2.5`
- **THEN** the API returns HTTP 422

#### Scenario: Null value falls back to default

- **WHEN** a podcast is created without an explicit `composeTemperature`
- **THEN** the persisted column is `NULL` and the compose stage uses `0.95`

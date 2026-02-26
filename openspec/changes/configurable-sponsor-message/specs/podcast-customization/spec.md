# Capability: Podcast Customization (Delta)

## ADDED Requirements

### Requirement: Sponsor configuration per podcast
Each podcast SHALL have an optional `sponsor` field (TEXT, nullable, stored as JSON map). The map keys SHALL be `name` and `message` (e.g., `{"name": "source-labs", "message": "experts in agentic software development"}`). When set, composers SHALL inject sponsor instructions into the prompt using the configured name and message. When null, no sponsor instructions SHALL appear in the prompt. The `sponsor` field SHALL be serialized to/from JSON using the same custom Spring Data JDBC converter pattern used for other JSON map fields.

#### Scenario: Podcast with sponsor configured
- **WHEN** a podcast has `sponsor` set to `{"name": "source-labs", "message": "experts in agentic software development"}`
- **THEN** the composer prompt includes instructions to mention the sponsor after the introduction and in the sign-off

#### Scenario: Podcast without sponsor
- **WHEN** a podcast has `sponsor` set to null
- **THEN** the composer prompt does not include any sponsor-related instructions

#### Scenario: Sponsor accepted in create endpoint
- **WHEN** a `POST /users/{userId}/podcasts` request includes `sponsor: {"name": "Acme Corp", "message": "building the future"}`
- **THEN** the podcast is created with the specified sponsor configuration

#### Scenario: Sponsor accepted in update endpoint
- **WHEN** a `PUT /users/{userId}/podcasts/{podcastId}` request includes `sponsor: {"name": "Acme Corp", "message": "building the future"}`
- **THEN** the podcast's sponsor is updated

#### Scenario: Sponsor included in GET response
- **WHEN** a `GET /users/{userId}/podcasts/{podcastId}` request is received for a podcast with `sponsor` set
- **THEN** the response includes the `sponsor` field with its JSON map value

#### Scenario: Sponsor defaults to null
- **WHEN** a podcast is created without specifying `sponsor`
- **THEN** the `sponsor` defaults to null

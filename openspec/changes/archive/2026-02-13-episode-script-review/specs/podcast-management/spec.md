## MODIFIED Requirements

### Requirement: Podcast entity scoped to user
The system SHALL store podcasts in a `podcasts` database table with columns: `id` (TEXT, primary key, UUID), `user_id` (TEXT, FK to users, NOT NULL), `name` (TEXT, NOT NULL), `topic` (TEXT, NOT NULL), and `require_review` (INTEGER, NOT NULL, default 0). The `id` SHALL be generated as a UUID v4 upon creation. A user MAY have multiple podcasts, each with a different topic. The `require_review` field controls whether episode scripts require manual review before TTS generation.

#### Scenario: Create a podcast
- **WHEN** a `POST /users/{userId}/podcasts` request is received with a JSON body containing `name` and `topic`
- **THEN** the system creates a podcast record linked to the user with a generated UUID and returns the created podcast with HTTP 201

#### Scenario: Create a podcast with review enabled
- **WHEN** a `POST /users/{userId}/podcasts` request is received with a JSON body containing `name`, `topic`, and `requireReview: true`
- **THEN** the system creates a podcast with `require_review` set to true

#### Scenario: Create podcast for non-existing user
- **WHEN** a `POST /users/{userId}/podcasts` request is received for a user that does not exist
- **THEN** the system returns HTTP 404

#### Scenario: Create podcast with missing fields
- **WHEN** a `POST /users/{userId}/podcasts` request is received without `name` or `topic`
- **THEN** the system returns HTTP 400 with a validation error message

### Requirement: Update podcast
The system SHALL allow updating a podcast's `name`, `topic`, and `requireReview` fields.

#### Scenario: Update podcast name and topic
- **WHEN** a `PUT /users/{userId}/podcasts/{podcastId}` request is received with a JSON body containing `name` and/or `topic`
- **THEN** the system updates the podcast record and returns HTTP 200 with the updated podcast

#### Scenario: Enable review on existing podcast
- **WHEN** a `PUT /users/{userId}/podcasts/{podcastId}` request is received with `requireReview: true`
- **THEN** the system updates the podcast's `require_review` to true; subsequent briefing generations will create pending episodes instead of auto-generating audio

#### Scenario: Update non-existing podcast
- **WHEN** a `PUT /users/{userId}/podcasts/{podcastId}` request is received for a podcast that does not exist or belongs to a different user
- **THEN** the system returns HTTP 404

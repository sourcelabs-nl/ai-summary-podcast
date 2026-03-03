## MODIFIED Requirements

### Requirement: Manual briefing generation per podcast
The system SHALL allow triggering briefing generation for a specific podcast via REST API, bypassing the cron schedule. When the podcast has `requireReview = true`, the manual trigger SHALL create an episode with status `PENDING_REVIEW` instead of immediately generating audio. The manual trigger SHALL return HTTP 409 (Conflict) if a `PENDING_REVIEW` or `APPROVED` episode already exists.

#### Scenario: Trigger manual briefing without review
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/generate` request is received for a podcast with `requireReview = false`
- **THEN** the system runs the LLM pipeline and TTS pipeline for that podcast and returns the result

#### Scenario: Trigger manual briefing with review
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/generate` request is received for a podcast with `requireReview = true`
- **THEN** the system runs the LLM pipeline, saves the script as an episode with status `PENDING_REVIEW`, and returns HTTP 200 indicating a script is ready for review

#### Scenario: Trigger manual briefing when pending exists
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/generate` request is received for a podcast with `requireReview = true` and a `PENDING_REVIEW` or `APPROVED` episode already exists
- **THEN** the system returns HTTP 409 (Conflict) indicating a pending script must be approved or discarded first

#### Scenario: Trigger manual briefing for non-existing podcast
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/generate` request is received for a podcast that does not exist
- **THEN** the system returns HTTP 404

#### Scenario: Manual briefing with no relevant articles
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/generate` request is received but the podcast has no relevant unprocessed articles
- **THEN** the system returns HTTP 200 with a message indicating no relevant articles to process

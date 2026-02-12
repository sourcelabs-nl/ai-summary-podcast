## MODIFIED Requirements

### Requirement: Briefing generation uses per-podcast schedule
The `BriefingGenerationScheduler` SHALL run on a fixed interval (e.g., every 60 seconds), query all podcasts, evaluate each podcast's `cron` expression against the current time and `last_generated_at`, and trigger the pipeline for podcasts that are due. When a podcast has `requireReview = true`, the scheduler SHALL skip generation if an episode with status `PENDING_REVIEW` or `APPROVED` already exists for that podcast.

#### Scenario: Scheduled briefing respects individual cron expressions
- **WHEN** the scheduler runs and podcast A (cron: daily 06:00, last generated yesterday) and podcast B (cron: daily 18:00, last generated today at 18:00) exist, and it is currently 06:05
- **THEN** the system triggers the pipeline for podcast A only

#### Scenario: Scheduled briefing with no podcasts
- **WHEN** the scheduler runs and no podcasts exist
- **THEN** the system completes without generating any episodes

#### Scenario: Newly created podcast runs on first check
- **WHEN** the scheduler runs and a podcast has `last_generated_at` set to null and its cron expression indicates it should have run already today
- **THEN** the system triggers the pipeline for that podcast

#### Scenario: Scheduled briefing skips when pending review exists
- **WHEN** the scheduler runs and a podcast with `requireReview = true` is due for generation, but an episode with status `PENDING_REVIEW` already exists for that podcast
- **THEN** the system skips generation for that podcast

#### Scenario: Scheduled briefing skips when approved episode exists
- **WHEN** the scheduler runs and a podcast with `requireReview = true` is due for generation, but an episode with status `APPROVED` already exists for that podcast
- **THEN** the system skips generation for that podcast

#### Scenario: Scheduled briefing generates when review required but no pending
- **WHEN** the scheduler runs and a podcast with `requireReview = true` is due for generation, and no episode with status `PENDING_REVIEW` or `APPROVED` exists for that podcast
- **THEN** the system triggers the pipeline and creates an episode with status `PENDING_REVIEW`

### Requirement: Manual briefing generation per podcast
The system SHALL allow triggering briefing generation for a specific podcast via REST API, bypassing the cron schedule. When the podcast has `requireReview = true`, the manual trigger SHALL create an episode with status `PENDING_REVIEW` instead of immediately generating audio. The manual trigger SHALL also skip generation if a `PENDING_REVIEW` or `APPROVED` episode already exists.

#### Scenario: Trigger manual briefing without review
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/generate` request is received for a podcast with `requireReview = false`
- **THEN** the system runs the LLM pipeline and TTS pipeline for that podcast and returns the result

#### Scenario: Trigger manual briefing with review
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/generate` request is received for a podcast with `requireReview = true`
- **THEN** the system runs the LLM pipeline, saves the script as an episode with status `PENDING_REVIEW`, and returns HTTP 200 indicating a script is ready for review

#### Scenario: Trigger manual briefing when pending exists
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/generate` request is received for a podcast with `requireReview = true` and a `PENDING_REVIEW` episode already exists
- **THEN** the system returns HTTP 409 (Conflict) indicating a pending script must be approved or discarded first

#### Scenario: Trigger manual briefing for non-existing podcast
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/generate` request is received for a podcast that does not exist
- **THEN** the system returns HTTP 404

#### Scenario: Manual briefing with no relevant articles
- **WHEN** a `POST /users/{userId}/podcasts/{podcastId}/generate` request is received but the podcast has no relevant unprocessed articles
- **THEN** the system returns HTTP 200 with a message indicating no relevant articles to process

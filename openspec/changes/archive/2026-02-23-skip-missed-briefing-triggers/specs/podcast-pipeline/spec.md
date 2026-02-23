## MODIFIED Requirements

### Requirement: Briefing generation uses per-podcast schedule
The `BriefingGenerationScheduler` SHALL run on a fixed interval (e.g., every 60 seconds), query all podcasts, evaluate each podcast's `cron` expression against the current time and `last_generated_at`, and trigger the pipeline for podcasts that are due. When a podcast has `requireReview = true`, the scheduler SHALL skip generation if an episode with status `PENDING_REVIEW` or `APPROVED` already exists for that podcast. The scheduler SHALL skip any cron trigger whose scheduled time is more than 30 minutes before the current time, logging a WARN-level message for each skipped trigger. The scheduler SHALL advance through all stale triggers to find the next actionable one without modifying `last_generated_at`.

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

#### Scenario: Missed trigger is skipped when system was down
- **WHEN** the scheduler runs and a podcast (cron: daily 15:00, last generated yesterday at 15:00) exists, and the current time is today at 18:00 (3 hours past the trigger)
- **THEN** the system skips the missed trigger, logs a WARN message, and does not generate an episode

#### Scenario: Trigger within staleness window is executed
- **WHEN** the scheduler runs and a podcast (cron: daily 15:00, last generated yesterday at 15:00) exists, and the current time is today at 15:10 (10 minutes past the trigger)
- **THEN** the system triggers the pipeline for that podcast

#### Scenario: Multiple missed triggers are skipped
- **WHEN** the scheduler runs and a podcast (cron: daily 15:00, last generated 3 days ago) exists, and the current time is today at 10:00
- **THEN** the system skips all 3 missed triggers (each logged at WARN level) and waits for today's 15:00 trigger

#### Scenario: Skipped triggers do not update last_generated_at
- **WHEN** the scheduler skips a stale trigger for a podcast
- **THEN** the podcast's `last_generated_at` remains unchanged

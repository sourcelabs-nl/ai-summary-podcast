## Why

When the application is down during a scheduled briefing trigger (e.g., system outage, maintenance), the scheduler retroactively fires the missed trigger on startup. This causes unintended automatic episode generation for a time slot that was missed. Missed triggers should be skipped, leaving it to the user to manually generate if needed. The next automatic generation should wait for the next cron trigger.

## What Changes

- The `BriefingGenerationScheduler` will skip cron triggers that are more than 30 minutes past their scheduled time (staleness window).
- When a trigger is missed, the scheduler fast-forwards through stale triggers to find the next future one, without updating `lastGeneratedAt`.
- A WARN-level log message is emitted for each skipped trigger so operators can see what was missed.

## Capabilities

### New Capabilities

_None — this change modifies an existing capability._

### Modified Capabilities

- `podcast-pipeline`: The briefing generation scheduler gains a staleness window; triggers older than 30 minutes are skipped instead of executed.

## Impact

- **Code**: `BriefingGenerationScheduler.kt` — scheduler loop logic changes.
- **Tests**: `BriefingGenerationSchedulerTest.kt` — new test scenarios for missed/stale triggers.
- **Data model**: No changes. `lastGeneratedAt` is not affected by skipped triggers.
- **APIs**: No changes.
## Context

The `BriefingGenerationScheduler` polls every 60 seconds, evaluates each podcast's cron expression against `lastGeneratedAt`, and triggers generation when the next execution time is in the past. This means if the system was down for hours or days, it will retroactively fire all missed triggers on startup.

Current scheduler logic:
```kotlin
val nextExecution = cronExpression.next(lastGenerated)
if (nextExecution != null && !nextExecution.isAfter(now)) {
    generateBriefing(podcast)
}
```

## Goals / Non-Goals

**Goals:**
- Skip cron triggers that are more than 30 minutes past their scheduled time.
- Preserve the semantic meaning of `lastGeneratedAt` — only updated when an episode is actually generated.
- Log skipped triggers at WARN level for observability.

**Non-Goals:**
- Making the staleness window configurable per podcast or via application properties.
- Automatically queuing missed episodes for later manual review.
- Changing the manual generation endpoint behavior.

## Decisions

### Fast-forward loop instead of updating `lastGeneratedAt`

When a trigger is stale, the scheduler walks forward through cron triggers until it finds one that is either in the future or within the staleness window:

```kotlin
var nextExecution = cronExpression.next(lastGenerated)
while (nextExecution != null && !nextExecution.isAfter(now)
       && Duration.between(nextExecution, now) > STALENESS_WINDOW) {
    log.warn("Skipping stale trigger at {} for podcast '{}'", nextExecution, podcast.name)
    nextExecution = cronExpression.next(nextExecution)
}
```

**Why not update `lastGeneratedAt`?** That field semantically represents "when an episode was last generated." Using it as a scheduler bookmark conflates two concerns and makes it harder to reason about podcast state.

**Why a loop?** The system could have been down for multiple cron cycles (e.g., a weekly podcast down for a month). The loop advances through all missed triggers cheaply since cron intervals are typically hours or days apart.

### Global 30-minute staleness window as a constant

The staleness window is a compile-time constant (`Duration.ofMinutes(30)`) in `BriefingGenerationScheduler`. Under normal operation, the scheduler picks up triggers within 60 seconds, so 30 minutes provides ample margin for slow startups or temporary delays without triggering generation for slots that were clearly missed.

**Alternative considered**: Application property or per-podcast field. Rejected because there's no current use case for varying this, and it can be extracted later if needed.

## Risks / Trade-offs

- **[Risk] System restarts within 30 minutes of a trigger still fire** → This is the intended behavior. A brief restart during the staleness window is not considered a "missed" trigger.
- **[Risk] Loop iteration count** → Bounded by the number of missed cron cycles. Even a daily podcast down for a year is ~365 iterations — negligible. No risk of infinite loops since `CronExpression.next()` always advances.
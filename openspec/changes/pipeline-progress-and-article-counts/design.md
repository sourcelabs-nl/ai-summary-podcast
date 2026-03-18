## Context

The podcast detail page "Next Episode" card is static — it shows article count and links to the upcoming page, but provides no feedback when the LLM pipeline is running. The article count also conflates posts with articles: nitter/twitter posts that will be aggregated into a single article are counted individually, making the number misleading. Additionally, the publish button remains visible even when all targets have been published to.

## Goals / Non-Goals

**Goals:**
- Emit real-time SSE events during each LLM pipeline stage so the frontend can show progress
- Show inline pipeline progress (spinner + stage label) on the "Next Episode" card
- Add a live countdown timer to the next scheduled cron-based generation
- Pre-calculate effective article count on the backend by simulating post-to-article aggregation
- Display both article and post counts when they differ
- Hide the publish button when an episode is fully published to all targets

**Non-Goals:**
- Detailed per-article progress (e.g., "scoring article 3 of 12")
- Persisting pipeline stage to the database
- Adding new publish targets

## Decisions

**1. Progress callback on LlmPipeline.run()**
Add an `onProgress` callback parameter (matching the existing `preview()` signature) rather than injecting the event publisher directly into the pipeline. This keeps the pipeline decoupled from Spring events — the caller (PodcastService) wires the callback to emit PodcastEvents.

**2. Pre-calculate article count on backend**
Compute effective article count in the controller using `SourceAggregator.shouldAggregate()` rather than duplicating aggregation logic on the frontend. Unlinked posts from aggregatable sources with >1 post count as 1 article. This reuses existing domain logic and keeps the frontend simple.

**3. Track "fully published" separately from "any published"**
Add a `fullyPublishedEpisodeIds` Set alongside the existing `publishedEpisodeIds`. The publish button hides when published to all TARGETS; the discard button hides when published to any target. Export the `TARGETS` constant from `publish-wizard.tsx` for reuse.

**4. Countdown timer using cron-parser**
Use the already-installed `cron-parser` library (v5) to compute the next cron run time client-side, updating every second via `setInterval`. The countdown hides during active pipeline progress.

## Risks / Trade-offs

- **[Stage events are fire-and-forget]** → If a stage completes very quickly, the frontend may not render it before the next stage event arrives. Acceptable since the final episode event always clears the progress state.
- **[Effective article count is an estimate]** → The pre-calculation simulates aggregation without actually running it. Edge cases (e.g., a source's `aggregate` flag changing between API call and generation) could cause slight discrepancies. Acceptable for a display-only count.

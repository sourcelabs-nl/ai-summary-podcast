## Context

The podcast pipeline generates episodes through a multi-stage LLM pipeline: source aggregation, article scoring/summarization, and script composition. Episodes are linked to their source articles via the `episode_articles` join table. Currently, once an episode is generated, there is no way to re-compose its script with different settings without running the full pipeline again (which requires unprocessed articles).

Users want to A/B test prompt changes (e.g. engagement techniques via `customInstructions`) by regenerating an existing episode's script using the same articles but with updated podcast configuration.

## Goals / Non-Goals

**Goals:**
- Allow re-composition of an existing episode's script using its original articles and current podcast settings
- Preserve the original episode (create a new one)
- Avoid side effects on the regular generation pipeline (no `lastGeneratedAt` drift, no article re-marking)

**Non-Goals:**
- Re-running scoring or aggregation stages — only composition is repeated
- Frontend UI for the regenerate action (API-only for now)
- Batch regeneration of multiple episodes

## Decisions

**1. Composition-only recompose method on LlmPipeline**
- Rationale: The existing `run()` method couples aggregation, scoring, and composition. A separate `recompose()` method isolates the composition step, taking pre-scored articles directly.
- Alternative: Adding flags to `run()` to skip stages — rejected as it would complicate the main pipeline path.

**2. Parameterize `createEpisodeFromPipelineResult` instead of duplicating**
- Rationale: The method handles episode saving, article linking, recap generation, show notes, and sources. Duplicating this for regeneration would create maintenance burden.
- Added `overrideGeneratedAt` (to inherit the source episode's date) and `updateLastGenerated` (to skip advancing the podcast timestamp). Defaults preserve existing behavior.

**3. Endpoint on PodcastController rather than EpisodeController**
- Rationale: Regeneration involves podcast-level concerns (current settings, LLM pipeline). The pattern `POST /{podcastId}/episodes/{episodeId}/regenerate` nests naturally under the podcast resource.

## Risks / Trade-offs

- [Risk] Regenerated episodes link to the same articles as the source episode → Multiple episodes may reference the same articles. This is acceptable since episode-article links are informational, not ownership-based.
- [Risk] `recompose()` uses the current previous episode recap, not the original one → The regenerated script may reference different continuity context. Acceptable for A/B testing purposes.

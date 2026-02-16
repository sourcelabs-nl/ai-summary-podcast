## Context

The `LlmPipeline.run()` method currently processes all unscored articles and composes a briefing without checking cost beforehand. Cost tracking exists (via `CostEstimator`) but is retrospective — costs are recorded after LLM calls complete. There is no mechanism to prevent a high-cost run from executing.

The pipeline has two LLM cost stages:
1. **Scoring/summarization** — one call per article using the `filter` model (cheap, e.g. gpt-4o-mini at $0.15/$0.60 per Mtok)
2. **Composition** — one call using the `compose` model (capable, e.g. claude-sonnet-4.5 at $3/$15 per Mtok)

## Goals / Non-Goals

**Goals:**
- Prevent unexpectedly expensive LLM pipeline runs by estimating cost before any API calls
- Make the cost threshold configurable per podcast with a sensible global default
- Keep the estimation simple and pessimistic (overestimate rather than underestimate)

**Non-Goals:**
- Precise token counting (no tokenizer dependency — use character-based heuristic)
- TTS cost gating (TTS cost scales with `targetWords` which is already bounded)
- Partial processing (process some articles within budget) — it's all or nothing
- UI for cost gate notifications (will be added later)

## Decisions

### Decision 1: Gate placement — before scoring (Gate A)

The cost check runs at the top of `LlmPipeline.run()`, after aggregation but before any LLM calls. This means no money is spent if the gate triggers.

**Alternative considered:** Gate before composition only (Gate B). Rejected because scoring many articles with a cheap model can still add up, and by then you've already spent money.

### Decision 2: Pessimistic estimation — assume all articles pass relevance filtering

The composition cost estimate assumes every article will be relevant and included in the briefing. This overestimates composition cost but avoids needing to predict relevance filtering outcomes.

**Alternative considered:** Use historical pass rates. Rejected as premature complexity — the pessimistic estimate is simpler and safer.

### Decision 3: Token estimation via `chars / 4`

Input tokens are estimated as `content.length / 4` (roughly 1 token per 4 characters for English text). No tokenizer library needed.

**Alternative considered:** Word-based estimation (`words × 1.3`). Both are rough — `chars / 4` is simpler to compute and avoids word-splitting.

### Decision 4: Fixed output token estimates

- Scoring output: **200 tokens** per article (small JSON with score + summary)
- Composition output: **`targetWords × 1.3`** tokens (script text, words to tokens)

These are estimates for cost gating, not exact counts.

### Decision 5: Prompt overhead absorbed by pessimism

System prompts add ~200-500 tokens per call but the pessimistic "all articles pass" assumption already overestimates enough to cover this. No explicit prompt overhead accounting.

### Decision 6: Add estimation method to `CostEstimator`

A new `estimatePipelineCostCents()` method on `CostEstimator` takes the list of articles, both model definitions, and target words, and returns the estimated total cost in cents. This keeps cost logic in one place.

### Decision 7: Per-podcast threshold with global fallback

New nullable `maxLlmCostCents` field on `Podcast`. Falls back to `app.llm.max-cost-cents` (default: 200). Follows the same pattern as `targetWords` falling back to `app.briefing.target-words`.

## Risks / Trade-offs

- **Over-estimation may skip valid runs** → The pessimistic approach means some runs that would actually be affordable get skipped. Users can raise their per-podcast threshold if this happens. Clear log messages will include the estimated cost so users can tune the threshold.
- **Under-estimation for non-English content** → The `chars / 4` heuristic works well for English but may underestimate tokens for CJK or emoji-heavy content. Acceptable risk — the pessimistic all-articles assumption provides buffer.
- **No notification mechanism** → When the gate triggers, it only logs. Users won't know unless they check logs. Mitigated by the plan to add UI notifications later.

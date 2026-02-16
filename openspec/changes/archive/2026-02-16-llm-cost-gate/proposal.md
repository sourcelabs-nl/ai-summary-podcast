## Why

The LLM pipeline currently has no cost guardrails — if a podcast accumulates many articles (e.g., high-volume RSS feeds or Twitter sources), the system will process them all regardless of cost. A single generation run could easily exceed $2+ in LLM API calls without warning. A pre-flight cost estimate that gates pipeline execution prevents unexpected spend.

## What Changes

- Add a cost estimation step at the start of `LlmPipeline.run()` that calculates the worst-case LLM cost (scoring + composition) before making any API calls.
- If the estimated cost exceeds the podcast's configured maximum (or a global default), skip the entire pipeline run and log a clear warning.
- Add a `maxLlmCostCents` field to the Podcast entity (nullable, falls back to global default).
- Add a global `app.llm.max-cost-cents` configuration property with a default of 200 (= $2.00).

## Capabilities

### New Capabilities
- `llm-cost-gate`: Pre-flight LLM cost estimation and pipeline gating based on configurable per-podcast cost thresholds.

### Modified Capabilities
- `podcast-customization`: Adding `maxLlmCostCents` field to podcast CRUD.

## Impact

- **Code**: `LlmPipeline`, `CostEstimator`, `Podcast` entity, `AppProperties`, podcast DTO/controller, database migration.
- **APIs**: Podcast create/update/get endpoints gain a new `maxLlmCostCents` field.
- **Config**: New `app.llm.max-cost-cents` property in `application.yaml`.
- **Database**: New `max_llm_cost_cents` column on `podcasts` table.
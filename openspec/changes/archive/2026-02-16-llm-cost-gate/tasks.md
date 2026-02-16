## 1. Configuration & Data Model

- [x] 1.1 Add `max-cost-cents` property to `LlmProperties` in `AppProperties.kt` (default: 200)
- [x] 1.2 Add `max-cost-cents: 200` to `application.yaml` under `app.llm`
- [x] 1.3 Add `maxLlmCostCents` field (nullable Int) to `Podcast` entity
- [x] 1.4 Create Flyway migration to add `max_llm_cost_cents` column (INTEGER, nullable) to `podcasts` table

## 2. API Layer

- [x] 2.1 Add `maxLlmCostCents` field to podcast create/update DTOs and response DTO
- [x] 2.2 Propagate `maxLlmCostCents` in podcast controller create/update/get mappings

## 3. Cost Estimation

- [x] 3.1 Add `estimatePipelineCostCents()` method to `CostEstimator` — takes article list, filter model def, compose model def, and target words; returns estimated cost in cents (nullable if pricing unavailable)
- [x] 3.2 Write unit tests for `estimatePipelineCostCents()` covering: normal estimation, varying article sizes, pricing not configured returns null

## 4. Pipeline Gating

- [x] 4.1 Add cost gate logic to `LlmPipeline.run()` — after aggregation, before scoring: estimate cost, compare to threshold, skip if exceeded
- [x] 4.2 Resolve effective threshold: `podcast.maxLlmCostCents ?: appProperties.llm.maxCostCents`
- [x] 4.3 Add WARN log when gate triggers, INFO log when proceeding with estimated cost
- [x] 4.4 Handle missing pricing (null estimate) — log warning and proceed
- [x] 4.5 Write unit tests for `LlmPipeline` cost gate: below threshold proceeds, above threshold skips, equal threshold proceeds, null pricing bypasses gate, per-podcast override respected

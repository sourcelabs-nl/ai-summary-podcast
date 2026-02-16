## Context

Source polling behaviour (`maxFailures`, `maxBackoffHours`, `maxArticleAgeDays`) is configured globally in `application.yaml` via `SourceProperties`. All sources and podcasts share these values. The codebase already has an established pattern for per-entity overrides with global fallback: nullable field on entity, resolved with `entity.field ?: appProperties.section.field` (used for `targetWords`, `maxLlmCostCents`, etc.).

The three properties are consumed in:
- `SourcePoller.poll()` — uses `maxArticleAgeDays` (content cutoff) and `maxFailures` (auto-disable threshold)
- `SourcePollingScheduler.isSourceDue()` — uses `maxBackoffHours` (backoff cap)
- `SourcePollingScheduler.pollSources()` — uses `maxArticleAgeDays` (cleanup cutoff)
- `LlmPipeline.run()` — uses `maxArticleAgeDays` (aggregation cutoff)

## Goals / Non-Goals

**Goals:**
- Allow per-source override of `maxFailures` and `maxBackoffHours`
- Allow per-podcast override of `maxArticleAgeDays`
- Follow the established nullable-field-with-global-fallback pattern
- Expose new fields via existing REST endpoints

**Non-Goals:**
- Cascading resolution (source → podcast → global) — each field lives at one entity level only
- Validation of ranges (e.g., min/max for maxFailures) — trust the user
- UI for these settings — API only

## Decisions

### Decision 1: `maxArticleAgeDays` on Podcast, `maxFailures` + `maxBackoffHours` on Source

`maxArticleAgeDays` controls content freshness window, which is a podcast-level concern (news vs weekly digest). `maxFailures` and `maxBackoffHours` control failure tolerance, which varies per source (flaky nitter vs reliable RSS).

**Alternative considered:** All three on Source. Rejected because article age is not a source-level concern — all sources feeding a podcast should use the same content window.

### Decision 2: Nullable fields with global fallback

Use `entity.field ?: appProperties.source.field` resolution. This matches the existing pattern for `targetWords`, `maxLlmCostCents`, etc. No new resolution mechanism needed.

### Decision 3: Single migration adding all three columns

One Flyway migration (V20) adds `max_article_age_days` to `podcasts` and `max_failures` + `max_backoff_hours` to `sources`. All nullable INTEGER columns.

### Decision 4: Pass resolved values to consuming code

Rather than passing `appProperties` and entities separately to `SourcePoller`/`SourcePollingScheduler`/`LlmPipeline`, resolve the effective value at the call site and pass it down. This keeps the resolution logic in the controller/scheduler layer.

## Risks / Trade-offs

- **No validation** — Users can set `maxFailures: 0` which would auto-disable on first failure. Acceptable — power users understand the tradeoff.
- **Global default change requires restart** — Per-entity overrides are in the database, but the global fallback still comes from `application.yaml`. This is consistent with all other config properties.

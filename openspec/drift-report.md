# OpenSpec Drift Report

_Generated: 2026-03-03_

## Summary Table

| # | Spec | Verdict | Notes |
|---|------|---------|-------|
| 1 | content-store | **IN SYNC** | All schema elements match |
| 2 | cost-tracking | **IN SYNC** | TTS + LLM cost estimation correct |
| 3 | llm-cache | **PARTIAL** | Surrogate PK instead of composite PK |
| 4 | llm-cost-gate | **IN SYNC** | Threshold logic, defaults, bypass all match |
| 5 | llm-processing | **PARTIAL** | Missing `includedPostIds`/`excludedPostIds` in scoring |
| 6 | model-registry | **IN SYNC** | Three-step resolution chain correct |
| 7 | pipeline-observability | **PARTIAL** | Missing relevant-count in batch summary log |
| 8 | podcast-pipeline | **PARTIAL** | Manual trigger returns 200 instead of 409 on conflict |
| 9 | dialogue-composition | **IN SYNC** | Prompt, speakers, sponsor, recap all match |
| 10 | interview-composition | **IN SYNC** | Tag roles, word ratios, transitions all match |
| 11 | source-config | **DRIFT** | Spec describes YAML config; impl is DB-driven. `max-failures` default 15 vs spec 5 |
| 12 | source-polling | **IN SYNC** | Host grouping, coroutines, dedup all correct |
| 13 | source-polling-backoff | **DRIFT** | `max-failures` default 15 vs spec 5 |
| 14 | source-aggregation | **IN SYNC** | Hybrid auto-detect, aggregation logic correct |
| 15 | source-labels | **IN SYNC** | Migration + entity + API all match |
| 16 | poll-rate-limiting | **IN SYNC** | Full precedence chain correct |
| 17 | post-store | **IN SYNC** | Schema, indexes, cross-source dedup all match |
| 18 | rss-category-filter | **IN SYNC** | Case-insensitive filter, passthrough on empty |
| 19 | twitter-oauth | **IN SYNC** | PKCE, signed state, token refresh all match |
| 20 | twitter-polling | **PARTIAL** | First-poll userId caching edge case |
| 21 | podcast-management | **IN SYNC** | CRUD, cascade delete, validation all match |
| 22 | podcast-customization | **IN SYNC** | All fields, style routing, validation match |
| 23 | podcast-feed | **IN SYNC** | RSS generation, enclosures, language all match |
| 24 | podcast-language | **IN SYNC** | All 57 language codes, locale resolution match |
| 25 | podcast-sources | **IN SYNC** | CRUD endpoints, schema, dedup constraint match |
| 26 | episode-review | **IN SYNC** | Status enum, transitions, discard logic match |
| 27 | episode-publishing | **IN SYNC** | Publisher registry, validation, publications table match |
| 28 | episode-continuity | **IN SYNC** | Recap generation, storage, token merging match |
| 29 | episode-article-tracking | **IN SYNC** | Join table, shared creation logic match |
| 30 | episode-articles-api | **IN SYNC** | Endpoint, response shape, nested source match |
| 31 | soundcloud-integration | **IN SYNC** | OAuth, PKCE, publisher, playlist all match (minor URL hostname diff) |
| 32 | tts-generation | **IN SYNC** | Chunking, silence prepend, concatenation match |
| 33 | tts-provider-abstraction | **IN SYNC** | Interface, factory, all 4 providers wired |
| 34 | tts-script-profile | **IN SYNC** | All providers return correct guidelines |
| 35 | elevenlabs-tts | **IN SYNC** | API client, dialogue batching, error handling match |
| 36 | elevenlabs-voice-discovery | **IN SYNC** | Voice listing, error codes match |
| 37 | inworld-tts | **PARTIAL** | `applyTextNormalization` sends `"ON"` vs spec scenario `true` — spec itself inconsistent |
| 38 | inworld-script-postprocessing | **IN SYNC** | All transforms in correct order |
| 39 | pronunciation-dictionary | **IN SYNC** | Schema, pipeline integration, guidelines match |
| 40 | user-management | **IN SYNC** | CRUD, cascade delete, versioning match |
| 41 | user-api-keys | **IN SYNC** | Encryption, provider defaults, env fallback match (minor: Ollama URL missing `/v1`) |
| 42 | frontend-dashboard | **PARTIAL** | Header layout order differs; "View" button is "Details" with wrong variant |
| 43 | frontend-podcast-settings | **PARTIAL** | Missing Integrations tab (5th tab with `soundcloudPlaylistId`) |
| 44 | frontend-publish-wizard | **PARTIAL** | Button widths/labels differ; no 409 conflict handling |
| 45 | frontend-source-management | **IN SYNC** | Table, dialogs, add/edit/delete all match |
| 46 | frontend-upcoming-episode | **IN SYNC** | Articles/Script tabs, generate button, word count match |
| 47 | episode-detail-page | **PARTIAL** | Missing "Edit Script" button for PENDING_REVIEW episodes |
| 48 | config-defaults-api | **IN SYNC** | All fields and alias resolution match |
| 49 | upcoming-articles-api | **IN SYNC** | Filtering, preview dry-run, response shape match |
| 50 | static-feed-export | **IN SYNC** | Triggers, fallback URL, error handling match |
| 51 | database-migrations | **PARTIAL** | V30 index names differ from spec; missing `IF NOT EXISTS` |
| 52 | code-review-agent | **IN SYNC** | Agent config, skills, rules all match |

## Totals

| Verdict | Count |
|---------|-------|
| IN SYNC | 36 |
| PARTIAL | 14 |
| DRIFT | 2 |

## Detailed Findings — DRIFT

### source-config

- Spec describes YAML-file-based source config via `@ConfigurationProperties`; implementation is entirely database-driven with REST API management
- `app.source.max-failures` default is **15** in code vs **5** in spec
- `pollIntervalMinutes` DB column default is 60 (baseline), though entity default is 30 (V29 updated rows but not column default)

### source-polling-backoff

- `app.source.max-failures` default is **15** in `AppProperties.kt` vs **5** in spec (tests use 5)

## Detailed Findings — PARTIAL

### llm-cache

- Spec says `(prompt_hash, model)` composite primary key; implementation uses surrogate auto-increment `id` PK with a `UNIQUE(prompt_hash, model)` constraint. Functionally equivalent but structurally different.

### llm-processing

- Spec requires scoring to return `includedPostIds` / `excludedPostIds` for aggregated articles. `ScoreSummarizeResult` only has `relevanceScore` and `summary` — no post-level filtering from LLM response.

### pipeline-observability

- Batch summary log `"[LLM] Article processing complete — 12 articles in 45.2s (8 relevant)"` is missing the `(N relevant)` count.
- Dialogue/Interview composers log `"Composing dialogue"` / `"Composing interview"` instead of the spec's uniform `"Composing briefing"`.

### podcast-pipeline

- Manual generate endpoint returns **HTTP 200** with a message when a pending/approved episode exists; spec requires **HTTP 409**.

### twitter-polling

- On first poll with no tweets returned, the userId is not cached in `lastSeenId` — username resolution repeats until tweets arrive.

### inworld-tts

- `applyTextNormalization` is sent as `"ON"` (string); spec scenario example shows `true` (boolean). The spec's own requirement text says `"ON"`, so the spec itself is internally inconsistent.

### frontend-dashboard

- Header layout: topic and cron are combined on one line (`{topic} · {cron}`) instead of cron on its own italic line
- Episode row "View" button is labeled "Details" using default variant instead of `outline` with "View" label
- Script viewer dialog removed from list page (moved to detail page)

### frontend-podcast-settings

- Missing 5th tab "Integrations" (with `soundcloudPlaylistId` field)

### frontend-publish-wizard

- Button widths use `w-24` not `w-20`; "Script" button doesn't exist on episode rows
- No specific 409 conflict error handling

### episode-detail-page

- Missing "Edit Script" button for `PENDING_REVIEW` episodes (backend endpoint exists)

### database-migrations

- V30 index names diverge significantly from spec (e.g., `idx_articles_source_score` vs spec's `idx_articles_podcast_processed`)
- Missing `IF NOT EXISTS` for idempotency

### user-api-keys (minor)

- Ollama default URL is `http://localhost:11434` in code vs `http://localhost:11434/v1` in spec

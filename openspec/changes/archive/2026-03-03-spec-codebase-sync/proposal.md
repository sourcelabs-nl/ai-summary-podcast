## Why

The drift report (2026-03-03) identified 16 discrepancies between OpenSpec specs and the codebase. 12 are specs lagging behind intentional code evolution; 5 are genuine code bugs. Bringing both sides in sync ensures specs remain a trustworthy source of truth.

## What Changes

**Spec updates (12):**
- `source-config`: Reflect DB-driven source management (not YAML), update `max-failures` default to 15
- `source-polling-backoff`: Update `max-failures` default to 15
- `llm-processing`: Remove `includedPostIds`/`excludedPostIds` requirement from scoring
- `llm-cache`: Change composite PK to surrogate PK + unique constraint
- `inworld-tts`: Fix scenario example from `true` to `"ON"` for `applyTextNormalization`
- `episode-detail-page`: Remove Edit Script button requirement
- `pipeline-observability`: Update composer log messages to use style-specific prefixes
- `database-migrations`: Match actual V30 index names, remove `IF NOT EXISTS` requirement
- `frontend-dashboard`: Match current UI layout (combined topic+cron line, "Details" button)
- `frontend-podcast-settings`: Remove Integrations tab requirement
- `frontend-publish-wizard`: Match current button widths/labels
- `user-api-keys`: Update Ollama default URL note (minor)

**Code fixes (5):**
- `podcast-pipeline`: Return HTTP 409 (not 200) when manual generate conflicts with existing pending/approved episode
- `pipeline-observability`: Add `(N relevant)` count to batch summary log
- `twitter-polling`: Cache userId in `lastSeenId` on first poll even when no tweets returned
- `user-api-keys`: Change Ollama default URL to `http://localhost:11434/v1`
- `frontend-publish-wizard`: Handle 409 conflict response in publish wizard UI

## Capabilities

### New Capabilities

_(none)_

### Modified Capabilities

- `source-config`: Update to DB-driven model, `max-failures` default 15
- `source-polling-backoff`: Update `max-failures` default to 15
- `llm-processing`: Remove post-level filtering from scoring stage
- `llm-cache`: PK structure change (surrogate + unique constraint)
- `inworld-tts`: Fix `applyTextNormalization` scenario value
- `episode-detail-page`: Remove Edit Script button
- `pipeline-observability`: Fix composer log prefix, add relevant count to batch summary
- `database-migrations`: Match actual index names
- `frontend-dashboard`: Match evolved UI layout
- `frontend-podcast-settings`: Remove Integrations tab
- `frontend-publish-wizard`: Match button styles, add 409 handling
- `podcast-pipeline`: HTTP 409 on conflict
- `twitter-polling`: First-poll userId caching
- `user-api-keys`: Ollama default URL

## Impact

- **Backend**: `PodcastController` (409 response), `LlmPipeline` (log message), `TwitterFetcher` (userId caching), `UserProviderConfigService` (Ollama URL)
- **Frontend**: Publish wizard (409 error handling)
- **Specs**: 14 spec files updated
- **No breaking changes**: All code fixes are backward-compatible behavior corrections

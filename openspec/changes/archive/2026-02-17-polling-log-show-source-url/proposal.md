## Why

Polling log messages currently display source UUIDs (e.g., `Source 422da499-b6e2-4714-b285-c75034424525 polled: 0 new posts saved`), which are meaningless without a database lookup. Replacing UUIDs with the source URL makes logs immediately actionable for debugging and monitoring.

## What Changes

- All `[Polling]` log messages in `SourcePoller` that reference `source.id` will instead display `source.url` (or both URL and type for disambiguation).

## Capabilities

### New Capabilities

_(none)_

### Modified Capabilities

- `source-polling`: Log messages will reference source URL instead of source UUID for human readability.

## Impact

- `SourcePoller.kt` — all log statements referencing `source.id` will be updated to use `source.url`
- No API, database, or dependency changes
- Existing tests that assert on log output may need updating
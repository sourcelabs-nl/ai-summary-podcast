## Overview

Replace `source.id` (UUID) with `source.url` in all `[Polling]` log messages in `SourcePoller.kt` so operators can identify sources at a glance without looking up UUIDs.

## Approach

Update all `log.info`, `log.warn`, and `log.error` calls in `SourcePoller.poll()` that currently reference `source.id` to use `source.url` instead. Since the `Source` object is already available in every log call, this is a straightforward parameter swap.

### Log message format

Current:
```
[Polling] Source 422da499-... polled: 0 new posts saved
```

New:
```
[Polling] Source https://example.com/feed polled: 0 new posts saved
```

## Changes

| File | Change |
|------|--------|
| `SourcePoller.kt` | Replace `source.id` with `source.url` in all log message placeholders |

## Risks

- None — log-only change with no behavioral impact.
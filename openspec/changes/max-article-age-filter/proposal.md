## Why

When a new source is added, the source poller fetches the entire feed — which can contain articles going back months or years. All of these get stored and processed into the next briefing, resulting in stale news being covered as if it were current. Articles older than a configurable threshold (default 7 days) are not relevant for a daily briefing and should be discarded at ingestion time.

## What Changes

- Add a configurable max article age property (`app.source.max-article-age-days`, default 7)
- Skip saving articles in `SourcePoller` when `publishedAt` is older than the configured max age
- Articles with `publishedAt = null` are still saved (age cannot be determined)
- Delete existing old unprocessed articles from the database on a periodic schedule

## Capabilities

### New Capabilities

_(none)_

### Modified Capabilities

- `source-polling`: Add max article age filtering during ingestion — articles with a `publishedAt` older than the configured threshold are skipped

## Impact

- `AppProperties.kt` — new `SourceProperties` with `maxArticleAgeDays`
- `SourcePoller.kt` — age check before saving articles
- `application.yml` — new config key
- `ArticleRepository.kt` — new query to delete old unprocessed articles
- `SourcePollingScheduler.kt` — call cleanup before polling

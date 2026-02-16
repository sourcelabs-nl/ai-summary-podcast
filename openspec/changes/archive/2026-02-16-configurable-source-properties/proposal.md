## Why

Source polling properties (`maxFailures`, `maxBackoffHours`) are currently global — every source shares the same failure tolerance and backoff cap. Flaky sources like Nitter need higher failure thresholds than reliable RSS feeds. Similarly, `maxArticleAgeDays` is global but should vary by podcast — a breaking-news podcast wants only fresh content (1-2 days) while a weekly digest benefits from a wider window (14 days). Making these configurable per-entity through the database enables fine-grained tuning without restarting the application.

## What Changes

- Add nullable `maxFailures` and `maxBackoffHours` fields to the `Source` entity, falling back to global defaults when null.
- Add a nullable `maxArticleAgeDays` field to the `Podcast` entity, falling back to the global default when null.
- Expose all three fields in their respective create/update/get API endpoints.
- Add Flyway migrations for the new columns.
- Update `SourcePoller`, `SourcePollingScheduler`, and `LlmPipeline` to resolve effective values from the entity → global fallback chain.

## Capabilities

### New Capabilities

(none)

### Modified Capabilities

- `source-config`: Adding `maxFailures` and `maxBackoffHours` as per-source overrides.
- `podcast-customization`: Adding `maxArticleAgeDays` as a per-podcast override.
- `source-polling-backoff`: Resolving `maxFailures` and `maxBackoffHours` from source entity before falling back to global config.

## Impact

- **Code**: `Source` entity, `Podcast` entity, `SourcePoller`, `SourcePollingScheduler`, `LlmPipeline`, `SourceController`, `PodcastController`, `AppProperties` (read sites).
- **APIs**: Source create/update/get endpoints gain `maxFailures` and `maxBackoffHours`. Podcast create/update/get endpoints gain `maxArticleAgeDays`.
- **Database**: Two new nullable INTEGER columns on `sources`, one on `podcasts`.

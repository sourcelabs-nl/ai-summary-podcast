## 1. Data Model & Migration

- [x] 1.1 Add `maxArticleAgeDays` field (nullable Int) to `Podcast` entity
- [x] 1.2 Add `maxFailures` and `maxBackoffHours` fields (nullable Int) to `Source` entity
- [x] 1.3 Create Flyway migration V20 to add `max_article_age_days` column to `podcasts` and `max_failures` + `max_backoff_hours` columns to `sources` (all nullable INTEGER)

## 2. Podcast API Layer

- [x] 2.1 Add `maxArticleAgeDays` field to podcast create/update DTOs and response DTO
- [x] 2.2 Propagate `maxArticleAgeDays` in podcast controller create/update/get mappings

## 3. Source API Layer

- [x] 3.1 Add `maxFailures` and `maxBackoffHours` fields to source create/update DTOs and response DTO
- [x] 3.2 Propagate `maxFailures` and `maxBackoffHours` in source controller create/update/get mappings

## 4. Resolution Logic

- [x] 4.1 Update `SourcePoller.poll()` to resolve `maxFailures` from `source.maxFailures ?: appProperties.source.maxFailures`
- [x] 4.2 Update `SourcePoller.poll()` to resolve `maxArticleAgeDays` — requires passing the podcast to the poller or resolving at the scheduler level
- [x] 4.3 Update `SourcePollingScheduler.isSourceDue()` to resolve `maxBackoffHours` from `source.maxBackoffHours ?: appProperties.source.maxBackoffHours`
- [x] 4.4 Update `SourcePollingScheduler.pollSources()` to resolve `maxArticleAgeDays` from `podcast.maxArticleAgeDays ?: appProperties.source.maxArticleAgeDays`
- [x] 4.5 Update `LlmPipeline.run()` to resolve `maxArticleAgeDays` from `podcast.maxArticleAgeDays ?: appProperties.source.maxArticleAgeDays`

## 5. Tests

- [x] 5.1 Update `SourcePollerTest` to verify per-source `maxFailures` override is respected
- [x] 5.2 Update `LlmPipelineTest` to verify per-podcast `maxArticleAgeDays` override is respected

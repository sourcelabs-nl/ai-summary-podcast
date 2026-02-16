## 1. Database Migration

- [x] 1.1 Create Flyway migration `V10__add_source_aggregate_column.sql` adding nullable boolean `aggregate` column to `sources` table (default null)

## 2. Source Entity & API

- [x] 2.1 Add `aggregate: Boolean?` field to `Source` data class in `store/Source.kt` (default null)
- [x] 2.2 Add `aggregate` field to `CreateSourceRequest`, `UpdateSourceRequest`, and `SourceResponse` in `SourceController.kt`
- [x] 2.3 Update `SourceService.create()` and `SourceService.update()` to pass through the `aggregate` field

## 3. SourceAggregator Component

- [x] 3.1 Create `SourceAggregator` component in `source/SourceAggregator.kt` with method `aggregate(articles: List<Article>, source: Source): List<Article>`
- [x] 3.2 Implement hybrid detection logic: check `source.aggregate` override, then auto-detect by source type and URL
- [x] 3.3 Implement digest article creation: merge titles, join bodies with separators, extract username, format date, pick most recent publishedAt

## 4. SourcePoller Integration

- [x] 4.1 Inject `SourceAggregator` into `SourcePoller` and call it after fetching articles, before the storage loop

## 5. Tests

- [x] 5.1 Write unit tests for `SourceAggregator`: multiple articles aggregated, single article unchanged, empty list unchanged, username extraction, hybrid detection logic
- [x] 5.2 Verify existing `SourcePoller` tests still pass
- [x] 5.3 Run full test suite with `./mvnw test`

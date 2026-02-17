## 1. Database & Entity

- [x] 1.1 Add Flyway migration V22 to add nullable `category_filter` TEXT column to `sources` table
- [x] 1.2 Add `categoryFilter` field to the `Source` entity

## 2. API Layer

- [x] 2.1 Add `categoryFilter` to `CreateSourceRequest` and `UpdateSourceRequest` DTOs
- [x] 2.2 Verify the field flows through `SourceService` create/update operations

## 3. RSS Fetcher

- [x] 3.1 Add `categoryFilter` parameter to `RssFeedFetcher.fetch()` and implement the category filtering logic (case-insensitive contains, uncategorized entries pass through)
- [x] 3.2 Pass `source.categoryFilter` from `SourcePoller` to `RssFeedFetcher.fetch()`

## 4. Tests

- [x] 4.1 Add unit tests for `RssFeedFetcher` category filtering (matching, non-matching, no categories, no filter)
- [x] 4.2 Add unit test for `SourcePoller` passing categoryFilter to the fetcher

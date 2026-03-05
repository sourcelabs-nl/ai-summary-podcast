## 1. Backend

- [x] 1.1 Add `getPostCounts(sourceIds)` batch method to `SourceService`
- [x] 1.2 Add `postCount` field to `SourceResponse` data class
- [x] 1.3 Wire post counts into `SourceController.list()` response mapping

## 2. Frontend

- [x] 2.1 Add `postCount` to `Source` interface in `types.ts`
- [x] 2.2 Update sources table in `sources-tab.tsx` to display post count

## 3. Tests

- [x] 3.1 Fix any broken tests due to the new `postCount` field

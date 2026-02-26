## 1. Database & Entity

- [x] 1.1 Add Flyway migration V10 to add nullable `sponsor` TEXT column to `podcasts` table
- [x] 1.2 Add `sponsor: Map<String, String>? = null` field to `Podcast` data class

## 2. API

- [x] 2.1 Add `sponsor` field to `CreatePodcastRequest`, `UpdatePodcastRequest`, and `PodcastResponse`
- [x] 2.2 Wire `sponsor` through controller create/update logic and `toResponse()` mapping

## 3. Composers

- [x] 3.1 Update `BriefingComposer` to conditionally inject sponsor instructions from `podcast.sponsor`
- [x] 3.2 Update `DialogueComposer` to conditionally inject sponsor instructions from `podcast.sponsor`
- [x] 3.3 Update `InterviewComposer` to conditionally inject sponsor instructions from `podcast.sponsor`

## 4. Tests

- [x] 4.1 Update composer tests to verify sponsor is injected when configured and omitted when null
- [x] 4.2 Update controller tests to verify sponsor field in create/update/get

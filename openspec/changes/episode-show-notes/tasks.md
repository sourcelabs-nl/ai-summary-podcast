## 1. Database

- [x] 1.1 Create Flyway migration to add `show_notes TEXT` column to `episodes` table

## 2. Backend: Show Notes Generation

- [x] 2.1 Add `showNotes` field to the `Episode` data class
- [x] 2.2 Create a `buildShowNotes(recap: String?, articles: List<Article>)` function that assembles plain text show notes (recap + "Sources:" + article title/URL lines)
- [x] 2.3 Call show notes generation in `EpisodeService.createEpisodeFromPipelineResult()` after recap is generated and episode-article links are saved, then save the updated episode

## 3. Backend: Use Show Notes in Descriptions

- [x] 3.1 Update `FeedGenerator` to use `episode.showNotes` for the RSS `<description>`, falling back to `scriptText.take(500) + "..."` when null
- [x] 3.2 Update `SoundCloudPublisher` to use `episode.showNotes` for the track description, falling back to current behavior when null

## 4. API and Frontend

- [x] 4.1 Add `showNotes` to the episode API response in `EpisodeController`
- [x] 4.2 Display show notes on the episode detail page in the frontend

## 5. Testing

- [x] 5.1 Test show notes generation (with recap + articles, without recap, without articles)
- [x] 5.2 Update `FeedGeneratorTest` for show notes in description
- [x] 5.3 Update `SoundCloudPublisherTest` for show notes in description
- [x] 5.4 Verify all existing tests pass

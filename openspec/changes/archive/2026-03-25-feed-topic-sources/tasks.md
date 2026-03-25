## 1. Database Migration

- [x] 1.1 Create Flyway migration to add nullable `topic` TEXT column to `episode_articles` table

## 2. Data Model and Repository

- [x] 2.1 Add `topic` field to `EpisodeArticle` data class
- [x] 2.2 Update `EpisodeArticleRepository.insertIgnore` to accept and persist `topic` parameter
- [x] 2.3 Update `FeedArticle` data class to include `topic` field
- [x] 2.4 Update `findArticlesByEpisodeIds` query to select `ea.topic` and include it in results

## 3. Pipeline Topic Mapping

- [x] 3.1 Add `articleTopics: Map<Long, String>` field to `PipelineResult`
- [x] 3.2 Build topic mapping from `DedupResult.clusters` in `LlmPipeline.run()` and `LlmPipeline.preview()`
- [x] 3.3 Set `articleTopics` to empty map in `LlmPipeline.recompose()`

## 4. Episode Service

- [x] 4.1 Update `EpisodeService.saveEpisodeArticleLinks` to accept topic map and pass topic per article to repository

## 5. Feed Generator

- [x] 5.1 Update `FeedGenerator.buildHtmlDescription` to group articles by topic and show one representative per topic
- [x] 5.2 Handle fallback: when all articles have null topic, show all articles (legacy behavior)

## 6. Tests

- [x] 6.1 Update `FeedGeneratorTest` to verify topic-grouped content:encoded output
- [x] 6.2 Update `FeedGeneratorTest` to verify fallback behavior for null-topic articles
- [x] 6.3 Update `EpisodeServiceTest` if `saveEpisodeArticleLinks` signature changed
- [x] 6.4 Run full test suite (`mvn test`) to verify no regressions

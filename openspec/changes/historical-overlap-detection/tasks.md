## 1. Configuration

- [ ] 1.1 Add `overlapLookbackEpisodes` (nullable Int) to `Podcast` entity and Flyway migration
- [ ] 1.2 Add `app.llm.overlap-lookback-episodes` (default: 5) to `AppProperties` (`LlmProperties`)
- [ ] 1.3 Add `overlapLookbackEpisodes` to podcast creation/update API DTOs

## 2. Data Access

- [ ] 2.1 Add `EpisodeArticleRepository.findArticleIdsByEpisodeIds(episodeIds)` query method
- [ ] 2.2 Add `EpisodeRepository.findRecentGeneratedByPodcastId(podcastId, limit)` query method to fetch N most recent GENERATED episodes
- [ ] 2.3 Add `ArticleRepository.findByIds(ids)` query method to fetch articles by a list of IDs

## 3. Overlap Detection Component

- [ ] 3.1 Create `OverlapDetector` component with method `detectOverlaps(candidates, recentArticles, filterModel): List<OverlapResult>` that calls the LLM
- [ ] 3.2 Write the overlap detection prompt template: structured input with "Previously published" and "New candidates" sections, JSON output format, follow-up story guidance
- [ ] 3.3 Parse LLM response into `OverlapResult` data class (article ID + reason)
- [ ] 3.4 Add unit tests for `OverlapDetector`: overlapping articles identified, follow-ups kept, empty input handling, malformed LLM response handling

## 4. Pipeline Integration

- [ ] 4.1 Add Stage 1b to `LlmPipeline.run()`: after scoring, query recent episode articles, call `OverlapDetector`, exclude overlapping candidates
- [ ] 4.2 Mark overlap-excluded articles as `is_processed = true` to prevent reprocessing
- [ ] 4.3 Include overlap detection token usage in `PipelineResult` totals
- [ ] 4.4 Add INFO logging for excluded articles and overlap check summary
- [ ] 4.5 Add unit tests for pipeline with overlap detection: overlap step skipped when no episodes, overlap-excluded articles marked processed, remaining articles composed into briefing

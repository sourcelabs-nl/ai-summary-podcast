## 1. Core Implementation

- [x] 1.1 Add article reset logic to `EpisodeController.discard()`: look up linked articles via `EpisodeArticleRepository.findByEpisodeId()`, then set `isProcessed = false` on each via `ArticleRepository.save()`

## 2. Tests

- [x] 2.1 Add unit test: discarding an episode resets `isProcessed` to `false` on all linked articles
- [x] 2.2 Add unit test: discarded episode's articles are picked up by the next pipeline run (verify `findRelevantUnprocessedBySourceIds` returns them)

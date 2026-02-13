## 1. Database Migration

- [x] 1.1 Create Flyway migration `V9__pipeline_rework.sql`: add `relevance_score` (INTEGER, nullable) to `articles`, migrate data from `is_relevant` (`true`→5, `false`→0, `null`→`null`), drop `is_relevant` column, add `relevance_threshold` (INTEGER, NOT NULL, default 5) to `podcasts`
- [x] 1.2 Update `Article` data class: replace `isRelevant: Boolean?` with `relevanceScore: Int?`
- [x] 1.3 Update `Podcast` data class: add `relevanceThreshold: Int = 5`

## 2. RSS Content Cleaning

- [x] 2.1 Update `RssFeedFetcher` to strip HTML from entry content/description using `Jsoup.parse(value).text()` before storing article body
- [x] 2.2 Add test for `RssFeedFetcher` verifying HTML is stripped from RSS entry bodies

## 3. Global Configuration

- [x] 3.1 Add `summarizationMinWords: Int = 500` to `LlmProperties` in `AppProperties` (config path: `app.llm.summarization-min-words`)

## 4. Pipeline Refactoring

- [x] 4.1 Create `RelevanceScorer` component: scores articles 0-10 with LLM prompt requesting `score` (0-10) + `justification`, persists `relevanceScore` per article immediately
- [x] 4.2 Create `ArticleSummarizer` component: summarizes articles with body word count >= `summarizationMinWords`, persists `summary` per article, skips short articles
- [x] 4.3 Update `LlmPipeline` to orchestrate three stages: (1) score unscored articles via `RelevanceScorer`, (2) summarize relevant unsummarized long articles via `ArticleSummarizer`, (3) compose briefing via `BriefingComposer`
- [x] 4.4 Update `BriefingComposer.buildPrompt` to use `article.summary ?: article.body` for each article's content block
- [x] 4.5 Remove or repurpose old `ArticleProcessor` (replaced by `RelevanceScorer` + `ArticleSummarizer`)

## 5. Repository Queries

- [x] 5.1 Update `ArticleRepository`: rename `findUnfilteredBySourceIds` to `findUnscoredBySourceIds` (query: `relevance_score IS NULL`), update `findRelevantUnprocessedBySourceIds` to use `relevance_score >= :threshold` instead of `is_relevant = 1`

## 6. Podcast API

- [x] 6.1 Update podcast create/update DTOs and controller to accept `relevanceThreshold`
- [x] 6.2 Update podcast GET response to include `relevanceThreshold`

## 7. Tests

- [x] 7.1 Add unit tests for `RelevanceScorer`: scoring, persistence, 0-10 range
- [x] 7.2 Add unit tests for `ArticleSummarizer`: summarization of long articles, skipping short articles, threshold boundary
- [x] 7.3 Update `LlmPipeline` tests for three-stage orchestration, including resume-after-crash scenarios
- [x] 7.4 Update `BriefingComposer` tests to verify it uses `summary` when present and `body` when summary is null
- [x] 7.5 Update any existing tests referencing `isRelevant` to use `relevanceScore`

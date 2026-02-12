## 1. Create ArticleProcessor

- [x] 1.1 Create `ArticleProcessingResult` data class with fields: `score: Int`, `justification: String`, `summary: String?`
- [x] 1.2 Create `ArticleProcessor` component that sends a single prompt per article with the podcast topic and full article body, parses the JSON response into `ArticleProcessingResult`, marks `isRelevant` and persists `summary` (only when score >= 3), and saves the article to the repository

## 2. Update LlmPipeline

- [x] 2.1 Replace `RelevanceFilter` and `ArticleSummarizer` dependencies in `LlmPipeline` with `ArticleProcessor`
- [x] 2.2 Simplify `LlmPipeline.run()` to call `articleProcessor.process()` on unfiltered articles, then pass results directly to `BriefingComposer` (removing the intermediate DB query for relevant unprocessed articles)

## 3. Remove old classes

- [x] 3.1 Delete `RelevanceFilter.kt` and `ArticleSummarizer.kt`

## 4. Update tests

- [x] 4.1 Write unit tests for `ArticleProcessor`: relevant article gets score + summary, irrelevant article gets score only, summary ignored when score < 3 even if LLM returns one, LLM error handling
- [x] 4.2 Update `LlmPipelineTest` to mock `ArticleProcessor` instead of `RelevanceFilter` and `ArticleSummarizer`

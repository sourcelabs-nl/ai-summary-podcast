## Why

The LLM pipeline currently makes two separate cheap-model calls per article: one for relevance scoring and one for summarization. Since most articles pass the relevance filter, this wastes a full round-trip per relevant article. Combining both into a single call reduces latency and API costs proportionally to the number of relevant articles.

## What Changes

- Merge `RelevanceFilter` and `ArticleSummarizer` into a single component (`ArticleProcessor`) that scores relevance and conditionally summarizes in one LLM call
- The prompt sends the full article body (not truncated to 2000 chars), asks for a relevance score + justification, and instructs the LLM to include a 2-3 sentence summary only when the score is >= 3
- Response format changes to a single JSON object: `{ score, justification, summary? }`
- Remove the separate `RelevanceFilter` and `ArticleSummarizer` classes
- Update `LlmPipeline` to call the single combined step instead of two sequential steps
- **BREAKING**: The `RelevanceFilter` and `ArticleSummarizer` Spring beans are removed (internal only, no external API impact)

## Capabilities

### New Capabilities

_(none)_

### Modified Capabilities

- `llm-processing`: The three-step pipeline becomes a two-step pipeline (combined relevance+summary, then briefing composition). The relevance filtering and summarization requirements merge into a single combined step.

## Impact

- **Code**: `RelevanceFilter.kt`, `ArticleSummarizer.kt` replaced by new `ArticleProcessor.kt`; `LlmPipeline.kt` simplified from three steps to two
- **Tests**: Existing tests for `RelevanceFilter` and `ArticleSummarizer` must be replaced with tests for the combined `ArticleProcessor`
- **Data model**: No schema changes — `Article` entity keeps `isRelevant`, `summary`, and `isProcessed` fields unchanged
- **API/External**: No external API changes — this is an internal pipeline optimization
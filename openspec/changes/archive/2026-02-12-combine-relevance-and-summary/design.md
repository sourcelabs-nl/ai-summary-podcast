## Context

The LLM pipeline currently runs three sequential steps per podcast: `RelevanceFilter` scores each article (1-5) using a truncated preview (2000 chars), `ArticleSummarizer` generates 2-3 sentence summaries of relevant articles using the full body, and `BriefingComposer` composes the final script. Both the filter and summarizer use the same cheap model, same temperature (0.3), and same `ChatClient` instance. Since most articles pass relevance (score >= 3), the two separate calls per relevant article are redundant.

## Goals / Non-Goals

**Goals:**
- Reduce LLM round-trips per relevant article from 2 to 1
- Maintain the same data model (`Article.isRelevant`, `Article.summary`) so downstream code (pipeline, composer, database) is unaffected
- Keep the ability to skip summarization for irrelevant articles (conditional in the prompt)

**Non-Goals:**
- Changing the briefing composition step
- Changing the data model or database schema
- Optimizing the prompt content itself (wording improvements are out of scope)
- Batching multiple articles into a single LLM call

## Decisions

### 1. Single combined prompt with conditional summary

The new `ArticleProcessor` sends a single prompt per article containing the full article body, the podcast topic, and instructions to: (a) score relevance 1-5 with justification, and (b) include a 2-3 sentence summary only if the score is >= 3.

**Rationale**: This is the simplest approach. The LLM naturally produces both outputs from the same reading of the article. For irrelevant articles, the summary field is null/omitted, so we don't waste output tokens on content we'd discard.

**Alternative considered**: Keep two separate calls but parallelize them. Rejected because it still doubles API costs and the sequential dependency (only summarize if relevant) makes true parallelism impossible.

### 2. JSON response format with optional summary field

Response structure:
```json
{ "score": 4, "justification": "Directly discusses AI tooling", "summary": "..." }
```

For irrelevant articles (score < 3), the summary field is omitted or null. Parsed via Spring AI's `entity()` mapping into a data class:
```kotlin
data class ArticleProcessingResult(
    val score: Int = 0,
    val justification: String = "",
    val summary: String? = null
)
```

**Rationale**: Extends the existing `RelevanceResult` pattern with one optional field. Spring AI's entity mapping handles optional fields cleanly.

### 3. Send full article body instead of 2000-char preview

The relevance filter currently truncates to 2000 chars. Since the summarizer already needs the full body, the combined call sends the full body for both purposes.

**Rationale**: Better relevance scoring from full context, and the tokens are sent regardless for summarization. No extra cost for relevant articles. Marginal extra input tokens for irrelevant articles.

### 4. Replace two classes with one

Delete `RelevanceFilter.kt` and `ArticleSummarizer.kt`, create `ArticleProcessor.kt` in the same package. Update `LlmPipeline.kt` to call the single step.

**Rationale**: Two classes with overlapping concerns (both iterate articles, call the LLM, save to DB) are better represented as one. The pipeline becomes simpler.

## Risks / Trade-offs

- **Slightly more input tokens for irrelevant articles**: Full body is sent instead of 2000-char preview. Since most articles are relevant, this is a net win. Mitigation: none needed, the savings on relevant articles far outweigh the marginal cost.
- **Conditional output reliability**: The LLM might include a summary even when score < 3. Mitigation: Ignore the summary field in code when `score < 3`, regardless of what the LLM returns.
- **LLM cache invalidation**: Existing cached relevance-only responses won't match the new combined prompt. Mitigation: Cache entries are keyed by prompt content, so new prompts naturally get new cache entries. Old entries expire naturally.
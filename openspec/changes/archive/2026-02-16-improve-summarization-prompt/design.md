## Context

The `ArticleScoreSummarizer` sends a prompt to the LLM that frames input as "Article title" / "Article text" and asks the LLM to "summarize the relevant content." This framing causes the LLM to produce meta-descriptions ("The article discusses...") instead of direct factual summaries. For aggregated posts (tweets/nitter entries), the framing is especially misleading — there is no "article," just a collection of short posts from a specific author.

The summaries feed directly into the briefing composer, so meta-description style degrades podcast script quality.

## Goals / Non-Goals

**Goals:**
- Eliminate meta-description patterns ("The article discusses...") from LLM summaries
- Provide content-type-aware context so the LLM understands whether it's summarizing an article or a collection of posts
- Include author identity in the prompt when available for natural attribution

**Non-Goals:**
- Changing the structured response format (relevanceScore, summary remain the same)
- Changing the briefing composition prompt
- Changing aggregation logic or article structure

## Decisions

### 1. Content-type-aware prompt framing

The prompt will detect whether the article is an aggregated post collection or a single article based on the article title prefix ("Posts from"). For aggregated articles, the prompt will state: "The following content consists of multiple social media posts by {author}." For single articles, it will use neutral framing: "Content title: ... Content: ..."

**Why**: The LLM produces better summaries when it understands the nature of the input. Calling a tweet digest "an article" confuses the model.

**Alternative considered**: Always use generic "content" framing regardless of type. Rejected because the explicit "social media posts" context produces noticeably better summaries for aggregated content.

### 2. Explicit negative example in summarization instruction

The prompt will include a concrete negative example: "Write directly about what happened — say 'Anthropic launched X' not 'The article discusses Anthropic launching X'."

**Why**: LLMs respond well to "do X, not Y" patterns. Without the negative example, they tend to revert to academic summary style.

### 3. Author name injected into prompt context

When `article.author` is non-null, the prompt will include it as context (e.g., "Content by @rauchg"). This is separate from the attribution preservation instruction that already exists.

**Why**: Giving the LLM the author name upfront produces summaries like "Guillermo Rauch highlights..." instead of "The author discusses..."

## Risks / Trade-offs

**[Prompt sensitivity]** → Prompt changes can have unpredictable effects across different LLM models. Mitigation: The changes are additive (more context, clearer instructions) rather than restrictive, reducing the risk of degraded output.

**[Detection heuristic]** → Using title prefix "Posts from" to detect aggregated articles is fragile if the title format changes. Mitigation: The title format is controlled by `SourceAggregator` and specified in the source-aggregation spec. If it changes, this detection should be updated accordingly.

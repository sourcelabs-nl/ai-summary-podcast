## Why

The LLM summarization stage produces meta-descriptions ("The article discusses...") instead of direct factual summaries ("Anthropic launched..."). This is especially problematic for aggregated posts (tweets, nitter entries) where there is no "article" — the content is a collection of short posts from a specific author. The meta-description style sounds unnatural when composed into a podcast script.

## What Changes

- **Reframe prompt context**: Replace "Article title" / "Article text" labels with content-type-aware framing. For aggregated posts, indicate that the content consists of multiple social media posts and name the author. For single articles, use neutral "content" framing.
- **Direct summarization instruction**: Add explicit instruction to state facts directly, with a negative example to prevent the "The article discusses..." pattern.
- **Author context in prompt**: Include the article's author name in the prompt when available, so the LLM can naturally attribute information.

## Capabilities

### New Capabilities

### Modified Capabilities
- `llm-processing`: Update the score+summarize+filter stage prompt to produce direct, factual summaries with content-type-aware framing and author context.

## Impact

- **ArticleScoreSummarizer**: Prompt construction changes — the prompt text sent to the LLM will change, but the structured response format (relevanceScore, summary) remains identical.
- **No API changes**: No external-facing changes. The improvement affects internal summary quality only.
- **No schema changes**: No database or configuration changes.

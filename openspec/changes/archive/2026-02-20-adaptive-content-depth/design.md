## Context

The LLM pipeline currently produces 2-3 sentence summaries for all articles regardless of length, and always feeds summaries (not bodies) to the compose step. On slow news days with few articles, this results in thin episodes under 5 minutes. The compose prompt already instructs the LLM not to pad with external knowledge, so the output length is naturally limited by the input material.

The `ArticleScoreSummarizer` handles scoring+summarization in a single LLM call. The three composers (`BriefingComposer`, `DialogueComposer`, `InterviewComposer`) all use `article.summary ?: article.body` when building the compose prompt.

## Goals / Non-Goals

**Goals:**
- Produce richer summaries for longer articles so the compose step has more material to work with.
- Use full article bodies instead of summaries when few articles are available, maximizing content depth.
- Keep configuration simple with sensible defaults.

**Non-Goals:**
- Dynamic token budget calculation — keeping it simple with article count threshold.
- Changing the compose prompt's target word count dynamically — the existing `targetWords` config is sufficient.
- Adding a minimum article count to skip episode generation — the existing behavior (skip when zero articles) is fine; even short episodes are acceptable.

## Decisions

### 1. Scale summary length by article body word count

The `ArticleScoreSummarizer` prompt will instruct the LLM to scale summary length based on the input content length:
- Short content (<500 words): 2-3 sentences (current behavior)
- Medium content (500-1500 words): 4-6 sentences
- Long content (1500+ words): a full paragraph covering key points and context

**Rationale:** This is a prompt-only change — no structural code changes needed. The LLM naturally adapts to the instruction. Longer summaries capture more nuance and give the compose step better material even when using summaries (many articles scenario).

**Alternative considered:** Fixed summary word count target (e.g., "summarize in 100 words"). Rejected because a word count target doesn't scale well — 100 words is too much for a 200-word post and too little for a 3000-word article.

### 2. Full body threshold as article count check in composers

The composers already use `article.summary ?: article.body`. The change is to make this decision based on article count: when the number of articles is below the threshold, use `article.body` for all articles (ignoring summaries). When above, use `article.summary ?: article.body` (current behavior).

This logic lives in the composer's `buildPrompt` method (and equivalents in `DialogueComposer`/`InterviewComposer`), since that's where article content is assembled into the prompt.

**Rationale:** The composers already have access to the article list and already decide which content to use. Adding a count check is minimal. Putting this in the composers (not `LlmPipeline`) keeps the decision close to where it matters and avoids changing the pipeline's interface.

**Alternative considered:** Passing a flag from `LlmPipeline` to the composers. Rejected as unnecessary indirection — the composers can count the articles themselves.

### 3. Configurable threshold with per-podcast override

Add `fullBodyThreshold` (default: 5) to `AppProperties.BriefingProperties` and an optional `fullBodyThreshold: Int?` to the `Podcast` entity. The composer reads `podcast.fullBodyThreshold ?: appProperties.briefing.fullBodyThreshold`.

**Rationale:** Follows the same pattern as `targetWords` — global default with per-podcast override. Users with high-volume sources may want a lower threshold (or 0 to always use summaries); users with few sources may want a higher threshold.

## Risks / Trade-offs

- **Increased compose-stage token cost when using full bodies** → Only triggers when article count is low, so total token volume stays bounded. A podcast with 3 long articles will cost more per-episode than one with 10 articles using summaries, but this is acceptable since it produces a better episode.
- **LLM may not follow summary length instructions precisely** → The word count boundaries (500/1500) are guidance, not hard constraints. Some variance is fine — the goal is "richer summaries for longer content," not exact word counts.
- **Full bodies may exceed context window with very long articles** → Unlikely with only a few articles (threshold defaults to 5), and the models used for composition typically support 100k+ tokens. Not a practical concern.

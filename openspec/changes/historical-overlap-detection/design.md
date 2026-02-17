## Context

The `content-overlap-safeguards` change introduces an `episode_articles` join table that tracks which articles contributed to each episode. This makes it possible to compare new candidate articles against previously published content. The current pipeline flow is: aggregate posts → score+summarize → compose briefing. There is no step that checks whether a candidate article covers a topic already covered in a recent episode.

## Goals / Non-Goals

**Goals:**
- Detect and exclude articles that semantically overlap with content from recent episodes before they reach the composition stage
- Keep the detection cheap by using the filter model (same model used for scoring) and summarized content
- Make the lookback window configurable per podcast

**Non-Goals:**
- Embedding-based similarity search — using the existing LLM infrastructure is simpler and sufficient
- Real-time overlap detection during polling — this operates at the LLM pipeline stage
- Preventing all overlap — some overlap is acceptable; the goal is to catch substantial repetition

## Decisions

### 1. LLM-based overlap detection as a pipeline step

**Decision**: Add an overlap detection step in `LlmPipeline` between scoring (Stage 1) and composition (Stage 2). This step sends candidate article summaries alongside recent episode article summaries to the filter model, asking it to identify which candidates substantially overlap with already-published content.

**Why**: The summaries produced by Stage 1 are concise (2-3 sentences each) and ideal for comparison. Using the LLM rather than embeddings avoids introducing a new dependency and leverages the existing model infrastructure. A single LLM call can compare all candidates against all recent articles at once.

**Alternative considered**: Embedding similarity with a threshold. Rejected because it requires an embedding model, a similarity computation layer, and tuning a threshold — more complexity for a problem that a cheap LLM call handles well.

### 2. Single batch call for overlap detection

**Decision**: Send one LLM call with all candidate article summaries and all recent episode article summaries. The LLM returns a list of candidate article IDs that should be excluded due to overlap.

**Why**: Sending one call instead of one-per-article is cheaper and faster. The total context is bounded: candidate summaries are 2-3 sentences each, and the lookback window limits historical summaries. Even with 20 candidates and 50 historical articles, the prompt fits easily within context.

**Format**: The LLM receives structured input with two sections — "Previously published" (grouped by episode) and "New candidates" — and returns a JSON array of overlapping candidate article IDs with a brief reason for each.

### 3. Configurable lookback window

**Decision**: Add `overlapLookbackEpisodes` to the `Podcast` entity (nullable, per-podcast override) and `app.llm.overlap-lookback-episodes` to `AppProperties` (global default, default: 5). The overlap check queries articles from the N most recent GENERATED episodes for the podcast.

**Why**: Different podcasts may have different publishing frequencies. A daily podcast may want to look back further than a weekly one. A global default of 5 episodes covers roughly a week for daily podcasts.

### 4. Skip overlap check when no recent episodes exist

**Decision**: If the podcast has no episodes with `status = GENERATED`, skip the overlap detection step entirely. Also skip if there are no candidate articles after scoring.

**Why**: No episodes means nothing to compare against. This is the common case for new podcasts and avoids a wasted LLM call.

## Risks / Trade-offs

**[Additional LLM cost per pipeline run]** → The overlap detection call uses the cheap filter model and processes only summaries (not full article bodies). Expected cost is minimal — comparable to scoring a single article. The call is skipped entirely when there are no recent episodes or no candidates.

**[False positives — legitimate follow-up stories excluded]** → The LLM prompt explicitly instructs the model to only flag articles that cover the *same* story/event, not follow-up stories with genuinely new information. The prompt includes guidance like: "An article about new developments in a previously covered story is NOT an overlap."

**[Dependency on `content-overlap-safeguards`]** → This change requires the `episode_articles` join table and `processedArticleIds` in `PipelineResult` to be implemented first. Without it, there's no way to know which articles went into which episode.

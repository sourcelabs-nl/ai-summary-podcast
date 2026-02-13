## Context

The pipeline currently makes LLM calls (scoring, summarization, composition) and TTS calls without tracking usage or cost. Spring AI's `ChatResponse` includes a `Usage` object with `promptTokens` and `generationTokens`, but `CachingChatModel` discards this metadata — it only stores the response text. Similarly, TTS character counts are never tracked.

The `articles` table holds per-article data and the `episodes` table holds per-episode data, making them natural places to store costs. The pipeline stages already return results that flow back to callers; adding token counts to these return values is straightforward.

## Goals / Non-Goals

**Goals:**
- Track LLM token usage (input + output tokens) per article (scoring, summarization) and per episode (composition)
- Track TTS character count per episode
- Estimate costs using configurable per-model pricing in `application.yaml`
- Expose cost data in existing API responses (episodes endpoint)
- Keep the approach simple — store aggregated costs on existing entities, no separate cost log table

**Non-Goals:**
- Real-time cost monitoring or alerting
- Per-request audit log table (separate `api_calls` table)
- Fetching dynamic pricing from OpenRouter API
- Budget limits or cost caps
- Historical cost reporting or aggregation endpoints

## Decisions

### 1. Store costs directly on articles and episodes (not a separate table)

**Decision**: Add token count and cost columns to `articles` and `episodes` tables.

**Rationale**: The user wants to see cost per article and cost per episode. Storing costs inline avoids joins and keeps queries simple. The data model matches the user's mental model directly.

**Alternative considered**: A separate `api_calls` table that logs every LLM/TTS call with full metadata (provider, model, tokens, cost, timestamp). This would be more flexible but adds complexity that isn't needed yet. Can be added later if needed.

### 2. Store both token counts and estimated cost

**Decision**: Store `input_tokens`, `output_tokens`, and `estimated_cost_cents` (integer, cents) on both articles and episodes. For TTS, store `tts_characters` and `tts_cost_cents` on episodes.

**Rationale**: Token counts are the "source of truth" and don't go stale. Estimated cost provides immediate utility. Using integer cents avoids floating-point issues.

### 3. Configurable model pricing in application.yaml

**Decision**: Add optional `input-cost-per-mtok` and `output-cost-per-mtok` (cost per million tokens in USD) to each model definition in the existing `app.llm.models` registry. TTS pricing is configured separately under `app.tts`.

**Rationale**: Pricing changes over time and varies by provider. Making it configurable keeps cost estimates accurate. Using per-million-token pricing matches how providers publish their rates.

**Example config:**
```yaml
app:
  llm:
    models:
      cheap:
        provider: openrouter
        model: openai/gpt-4o-mini
        input-cost-per-mtok: 0.15
        output-cost-per-mtok: 0.60
      capable:
        provider: openrouter
        model: anthropic/claude-sonnet-4.5
        input-cost-per-mtok: 3.00
        output-cost-per-mtok: 15.00
  tts:
    cost-per-million-chars: 15.00
```

### 4. Extract usage from ChatResponse in CachingChatModel

**Decision**: Modify `CachingChatModel.call()` to extract `Usage` from `ChatResponse.metadata` and return a response that preserves this metadata. On cache hits, usage is zero (no API cost incurred).

**Rationale**: `CachingChatModel` is the single point through which all LLM calls flow. Extracting usage here ensures consistent tracking. Cache hits correctly report zero tokens since no actual API call was made.

### 5. Accumulate article costs across scoring + summarization

**Decision**: After scoring, save the scoring token counts on the article. After summarization, add the summarization token counts to the article's existing counts. The article's `estimated_cost_cents` reflects the sum of both stages.

**Rationale**: An article goes through up to two LLM calls. Accumulating gives a total per-article cost that answers "how much did this article cost to process?"

### 6. Episode cost = composition LLM cost + TTS cost

**Decision**: The episode's LLM cost comes from the composition stage. TTS cost is derived from `tts_characters * tts_cost_per_million_chars / 1_000_000`. The total episode cost does not include the per-article costs (those are on the articles).

**Rationale**: Keeps the cost attribution clean — articles track their own LLM costs, episodes track composition + audio costs. Users can sum them if they want a full pipeline cost.

## Risks / Trade-offs

- **[Risk] Spring AI ChatResponse may not always include Usage metadata** → Some providers or cached responses may return null usage. Mitigation: treat null usage as 0 tokens. Log a warning on the first occurrence per model.
- **[Risk] Pricing config becomes stale** → Model pricing changes over time. Mitigation: costs are clearly labeled as "estimated." Token counts are always accurate and can be recomputed.
- **[Risk] Cache hits report zero cost** → If an article is scored via cache hit, its cost will be 0 even though the original call had a cost. Mitigation: this is correct behavior — the cache hit genuinely cost nothing. The original call's cost was attributed to whichever article triggered the cache fill.
- **[Trade-off] No separate cost log** → We can't query "all API calls in the last 24 hours." This is acceptable for the current scope.
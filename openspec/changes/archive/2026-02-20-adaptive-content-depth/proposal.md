## Why

When few articles are available (e.g., slow news day), the compose step receives only short 2-3 sentence summaries as input, resulting in thin episodes that may be under 5 minutes. Now that deduplication and polling bugs are fixed, real content volume is lower than expected, making this a practical problem.

## What Changes

- Scale summary length based on article body length: short articles get 2-3 sentences (current), medium articles get 4-6 sentences, long articles get a full paragraph with key points and context.
- When the number of relevant articles is below a configurable threshold (default: 5), the compose step uses full article bodies instead of summaries, giving the LLM richer material to produce a meaningful episode.
- The article count threshold is configurable per-podcast (with a global default) so users can tune the behavior.

## Capabilities

### New Capabilities

_None — this change modifies existing capabilities only._

### Modified Capabilities

- `llm-processing`: The score+summarize stage changes to produce longer summaries for longer articles. The composition stage changes to use full article bodies when article count is below a threshold.

## Impact

- `ArticleScoreSummarizer` — prompt changes to scale summary length with article body length.
- `BriefingComposer`, `DialogueComposer`, `InterviewComposer` — logic to select summary vs. full body based on article count threshold.
- `LlmPipeline` — passes article count context to composers so they can decide which content to use.
- `Podcast` entity / `AppProperties` — new `fullBodyThreshold` configuration field.
- Compose-stage token costs will increase when full bodies are used, but this only happens when article count is low, naturally balancing overall cost.

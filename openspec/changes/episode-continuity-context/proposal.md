## Why

Podcast episodes are generated independently with no awareness of what was previously discussed. This makes each episode feel disconnected. Real podcasts naturally reference previous episodes — "as we mentioned last time" or "following up on yesterday's story." Adding continuity context to the composition step makes the podcast sound more natural and helps listeners follow evolving stories.

## What Changes

- **Previous episode recap generation**: Before composing a new briefing, generate a short (2-3 sentence) recap of the most recent episode using the filter/cheap model. This recap is passed to the composer as context.
- **Continuity-aware composition prompts**: Both `BriefingComposer` and `DialogueComposer` receive the recap and are instructed to reference the previous episode. When today's topics overlap with the previous episode, the script weaves in specific references ("last time we covered X, and today there are new developments..."). When there is no topical overlap, the script includes a brief one-liner ("last episode we discussed X and Y, today we're looking at...").
- **Always on**: Continuity context is included whenever a previous episode exists. No configuration toggle.

## Capabilities

### New Capabilities
- `episode-continuity`: Generation of a recap from the previous episode and injection of continuity context into the composition prompt, enabling natural back-references across episodes.

### Modified Capabilities
- `llm-processing`: The pipeline gains an episode recap step before composition. Both monologue and dialogue composers receive previous-episode context.
- `dialogue-composition`: The dialogue prompt is extended with previous-episode recap context and continuity instructions.

## Impact

- **LLM pipeline**: One additional cheap-model LLM call per pipeline run (when a previous episode exists) to generate the recap.
- **Cost**: Minimal — the recap call uses the filter model and processes a single episode script into 2-3 sentences.
- **Composers**: Both `BriefingComposer` and `DialogueComposer` prompts are extended with a recap section and continuity instructions.
- **Database queries**: One query to fetch the most recent GENERATED episode for the podcast.

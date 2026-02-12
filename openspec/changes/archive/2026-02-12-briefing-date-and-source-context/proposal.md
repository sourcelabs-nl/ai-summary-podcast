## Why

Podcast briefings currently lack contextual grounding — listeners don't know when the episode was generated, what podcast they're listening to, or where the information comes from. Adding the current date, podcast name/topic, and subtle source attribution to the briefing introduction makes the content more informative and trustworthy.

## What Changes

- Pass the current date to the BriefingComposer prompt and instruct the LLM to mention it in the introduction
- Pass the podcast name and topic to the prompt and instruct the LLM to mention them in the introduction
- Include source names/URLs in the article summary block so the LLM can subtly reference where information comes from throughout the script
- Update prompt instructions to require date, podcast identity, and source attribution

## Capabilities

### New Capabilities

_None_

### Modified Capabilities

- `llm-processing`: The briefing script composition requirement changes to include current date, podcast name/topic, and source attribution in the prompt context and instructions

## Impact

- `BriefingComposer.kt` — prompt enrichment with date, podcast name/topic, and article source metadata
- No API changes, no database changes, no new dependencies

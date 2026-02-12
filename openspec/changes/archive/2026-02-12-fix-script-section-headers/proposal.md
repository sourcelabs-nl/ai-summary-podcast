## Why

The BriefingComposer prompt tells the LLM not to include "stage directions, sound effects, or non-spoken text", but LLMs frequently still generate section headers like `[Opening]`, `[Transition]`, `[Closing]` in the briefing script. These bracketed headers end up in the TTS audio as spoken text, degrading the listener experience.

## What Changes

- Strengthen the BriefingComposer prompt to explicitly prohibit bracketed section headers (e.g., `[Opening]`, `[Closing]`, `[Transition]`)
- Add a post-generation regex cleanup step to strip any remaining `[...]` headers from the script as a safety net

## Capabilities

### New Capabilities

_None_

### Modified Capabilities

- `llm-processing`: The briefing script composition requirement changes to explicitly prohibit bracketed section headers and require post-generation sanitization

## Impact

- `BriefingComposer.kt` — prompt text update and added script cleanup logic
- No API changes, no database changes, no new dependencies
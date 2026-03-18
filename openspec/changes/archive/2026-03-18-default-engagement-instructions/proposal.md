## Why

The podcast custom instructions for "The Daily Agentic AI Podcast" contain engagement techniques (hook openings, curiosity hooks, mid-sentence interruptions, front-loading best stories, callbacks, short segments) that produce significantly better scripts. These techniques are universally good for interview and dialogue styles and should be built into the default composer prompts rather than requiring each podcast to rediscover them via custom instructions.

Additionally, the current prompts lack guidance on emphasizing important announcements, and there's a conflict between the "no stage directions" rule and TTS-supported tags (e.g., Inworld's `[laugh]`, `[sigh]`).

## What Changes

- Add engagement techniques to `InterviewComposer` default prompt: hook opening, front-load best story, curiosity hooks, mid-roll callbacks, short segments with signposting, natural mid-sentence interruptions, emphasis on important announcements
- Add equivalent engagement techniques to `DialogueComposer` default prompt (adapted for multi-speaker)
- Add applicable subset to `BriefingComposer` default prompt (hook opening, front-load best story, short segments, emphasis — no interruptions for single speaker)
- Clarify "no stage directions" rule in all three composers to explicitly permit TTS-supported cues inside speaker tags
- Clear custom instructions on "The Daily Agentic AI Podcast" (the techniques are now defaults)

## Capabilities

### New Capabilities

_None — this change enhances existing capabilities._

### Modified Capabilities

- `interview-composition`: Add engagement techniques (hook opening, front-load best story, curiosity hooks, mid-roll callbacks, short segments, natural interruptions, emphasis) and TTS tag clarification to default prompt
- `dialogue-composition`: Add engagement techniques (adapted for multi-speaker) and TTS tag clarification to default prompt

## Impact

- `InterviewComposer.kt` — prompt text changes
- `DialogueComposer.kt` — prompt text changes
- `BriefingComposer.kt` — prompt text changes
- `InterviewComposerTest.kt` — update prompt assertions if any
- `DialogueComposerTest.kt` — update prompt assertions if any
- `BriefingComposerTest.kt` — update prompt assertions if any
- Production data — clear `customInstructions` field on podcast `85b9d107-f608-45be-a8f6-3ed1f731967a`

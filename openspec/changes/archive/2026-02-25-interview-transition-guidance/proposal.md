## Why

Interview-style podcast scripts have unnatural speaker transitions — the LLM tends to start turns with bare name addresses ("Jarno, the market has...") rather than natural conversational bridges ("What do you think about this, Jarno?"). This makes the TTS output sound stilted at handover points. The current prompt has no guidance on transition phrasing.

## What Changes

- Add explicit transition and handover guidance to the `InterviewComposer` prompt, instructing the LLM to use natural conversational bridges, varied transition patterns, and avoid bare name-first openings

## Capabilities

### New Capabilities

_None_

### Modified Capabilities

- `interview-composition`: Add requirements for natural speaker transition phrasing in the interview prompt

## Impact

- **Code**: `InterviewComposer.kt` — additional bullet points in the prompt's Requirements section
- **APIs**: None
- **Dependencies**: None

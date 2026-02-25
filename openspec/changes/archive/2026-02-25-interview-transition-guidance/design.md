## Context

The `InterviewComposer` prompt tells the LLM to have speakers "use each other's names naturally in conversation" but provides no guidance on transition phrasing. This results in repetitive, stilted patterns where turns begin with a bare name address ("Jarno, ...") rather than natural conversational bridges.

## Goals / Non-Goals

**Goals:**
- Improve the naturalness of speaker transitions in interview-style scripts by adding prompt guidance

**Non-Goals:**
- Changing the 20/80 word split or interview structure
- Modifying `DialogueComposer` (separate change if needed)
- Changing TTS behavior or markup

## Decisions

### 1. Prompt-only change

**Decision:** Add transition guidance as additional bullet points in the existing Requirements section of the `InterviewComposer` prompt. No code structure changes.

**Why:** The issue is purely about what the LLM generates. The fix is prompt engineering — giving the LLM explicit examples and anti-patterns for transitions.

### 2. Guidance content

The prompt additions should cover:
- **Anti-pattern**: Don't start turns with bare name addresses ("Jarno, the market...")
- **Bridge phrases**: Use reactions, follow-ups, and conversational connectors before transitioning ("That's really interesting. Now I'm curious about...", "Great point. Speaking of which...")
- **Name placement**: Names should appear mid-sentence or at the end of questions, not as turn openers
- **Variation**: Vary transition patterns — not every handover needs to use the other speaker's name

## Risks / Trade-offs

- **[Risk] Over-constraining the LLM** → Keep guidance to 3-4 bullet points, not a rigid formula. The goal is to nudge away from bad patterns, not script every transition.
- **[Risk] Guidance ignored by some models** → Some smaller models may not follow nuanced prompt instructions as well. Acceptable — the guidance improves output for capable models without breaking anything for others.

## Context

The `InterviewComposer` generates interview-style podcast scripts via a single LLM prompt. The prompt defines speaker roles (interviewer ~20%, expert ~80%), engagement techniques (hook opening, curiosity hooks, mid-roll callbacks, natural interruptions), and structural rules (alternating XML tags, no stage directions). The current prompt already asks for "natural interruptions" and "3-5 sentences max" per expert turn, but the LLM consistently produces long expert monologues because the instruction is too soft and competes with the 80% word allocation.

## Goals / Non-Goals

**Goals:**
- Shorter, punchier expert turns through stricter turn-length enforcement and a 65/35 word split
- A "coming up" teaser after the sponsor that previews interesting topics (conditional on article count)
- 2-3 strategic cliffhangers between stories to maintain attention across transitions
- Varied, spontaneous-feeling interruptions (excited, skeptical, confused, connecting dots, disagreement)

**Non-Goals:**
- Changing the TTS pipeline or audio processing
- Adding new speaker roles or modifying the XML tag structure
- Changing the dialogue or briefing composers
- Making the teaser/cliffhanger features configurable per-podcast (they're prompt-level improvements)

## Decisions

**1. Prompt-only changes — no code structure changes**
All changes are modifications to the prompt text in `InterviewComposer.buildPrompt()`. No new classes, services, or configuration properties needed. The article count check for the teaser is done in Kotlin before building the prompt string.

*Rationale:* These are all LLM instruction changes. Adding code complexity for what is fundamentally a prompt improvement would be over-engineering.

**2. Conditional teaser based on article count (5+)**
The "coming up" teaser instruction is only included when `articles.size >= 5`. With fewer articles, previewing all topics upfront removes the element of surprise.

*Alternative considered:* Making the threshold configurable per podcast — rejected as premature. Can be added later if needed.

**3. Cliffhanger count fixed at 2-3**
The prompt asks for exactly 2-3 strategic cliffhangers. Too few has no effect; too many feels formulaic.

**4. Interruption variety as explicit examples**
Rather than just saying "varied interruptions", the prompt provides 5 specific interruption types with examples. This gives the LLM concrete patterns to draw from while still allowing creative variation.

## Risks / Trade-offs

- **[Risk] LLM may still produce long expert turns despite stricter instruction** → Mitigation: The 65/35 split mechanically reduces expert airtime, and framing the turn-length rule as a "hard rule" (not a suggestion) with the reason ("listener drop-off") makes the LLM more likely to comply
- **[Risk] Cliffhangers may feel forced with few articles** → Mitigation: The instruction says "2-3 per episode" which gives the LLM flexibility to use fewer when there isn't enough content to tease
- **[Risk] "Coming up" teaser may not match actual episode content** → Mitigation: Sonnet 4.6 (the compose model) handles planning-ahead well; the teaser is written before the stories, so the LLM knows the full article set

## Why

The interview-style podcast script produces expert turns that are too long and monologue-like, causing listener fatigue. The show lacks structural variety — no topic preview after the sponsor, no cliffhangers between stories, and interruptions feel scripted rather than spontaneous. These are standard radio/podcast engagement techniques that would make the show significantly more compelling.

## What Changes

- Shift speaker word distribution from 80/20 (expert/interviewer) to 65/35 — more interviewer airtime naturally breaks up expert monologues
- Add a "coming up" teaser segment after the sponsor message (only when 5+ articles are covered) previewing 3-4 interesting topics
- Add 2-3 strategic cliffhangers between stories to maintain listener attention across topic transitions
- Replace the current "natural interruptions" instruction with richer, varied spontaneous interruptions: excited reactions, skeptical pushback, audience-surrogate confusion, connecting dots, and playful disagreement
- Enforce stricter expert turn length (3-4 sentences max as a hard rule, not a suggestion)

## Capabilities

### New Capabilities

_(none)_

### Modified Capabilities

- `interview-composition`: Change word distribution to 65/35, add "coming up" teaser, add strategic cliffhangers, add varied spontaneous interruptions, enforce stricter turn length

## Impact

- `InterviewComposer.kt` — prompt text changes (word distribution, new engagement techniques, stricter turn rules)
- `InterviewComposerTest.kt` — new/updated tests for teaser, cliffhangers, interruption variety, and turn length enforcement
- `interview-composition` spec — updated requirements reflecting the new engagement techniques

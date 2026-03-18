## Context

The three composer classes (`InterviewComposer`, `DialogueComposer`, `BriefingComposer`) each build an LLM prompt with hardcoded requirements. Custom instructions are appended at the end via `podcast.customInstructions`. A user has developed a set of engagement techniques in custom instructions that produce significantly better scripts — these should become defaults.

Each TTS provider declares `scriptGuidelines()` that get injected into the prompt. The Inworld provider supports `[laugh]`, `[sigh]`, `*emphasis*` etc., but the composer prompts say "no stage directions or non-spoken text", creating an ambiguity.

## Goals / Non-Goals

**Goals:**
- Add engagement techniques to default interview/dialogue/briefing prompts so all podcasts benefit
- Resolve the TTS tag conflict between "no stage directions" and provider-specific emotion/non-verbal tags
- Clear custom instructions on the production podcast that pioneered these techniques

**Non-Goals:**
- Changing the composer architecture or code structure — only prompt text changes
- Adding new configuration options or API fields
- Modifying TTS provider behavior

## Decisions

### Prompt text changes only
All changes are to string literals inside `buildPrompt()` methods. No new classes, fields, or architectural changes needed. This keeps the blast radius minimal.

### Engagement techniques adapted per composer type

| Technique | Interview | Dialogue | Briefing |
|---|---|---|---|
| Hook opening | Yes | Yes | Yes |
| Front-load best story | Yes | Yes | Yes |
| Curiosity hooks | Yes (interviewer) | Yes (host) | Yes |
| Mid-roll callbacks | Yes | Yes | Yes |
| Short segments | Yes | Yes | Yes |
| Natural interruptions | Yes (interviewer interrupts expert) | Yes (speakers interrupt each other) | No (single speaker) |
| Emphasis on important news | Yes | Yes | Yes |

### TTS tag clarification approach
Amend the "no stage directions" line to explicitly carve out TTS-supported cues when they appear inside speaker tags. This is provider-agnostic — the TTS guidelines section (when present) defines what cues are supported.

### Production data cleanup
Clear `customInstructions` on podcast `85b9d107-f608-45be-a8f6-3ed1f731967a` via API call after code changes are deployed and verified.

## Risks / Trade-offs

- **Prompt length increase** → ~150-200 extra words per composer prompt. Negligible impact on token cost since articles dominate.
- **LLM compliance varies** → Not all models follow every instruction perfectly. The techniques are guidance, not guarantees. Mitigated by using imperative language ("MUST", "SHALL").
- **Existing podcasts get new behavior** → Scripts will change for all existing podcasts. This is intentional — the techniques are improvements. Users who dislike a specific technique can override via custom instructions.

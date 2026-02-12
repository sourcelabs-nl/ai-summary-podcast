## Context

The `BriefingComposer` generates podcast scripts by prompting an LLM with article summaries. The current prompt (line 49 in `BriefingComposer.kt`) instructs the LLM: "Do NOT include any stage directions, sound effects, or non-spoken text". Despite this, LLMs frequently generate bracketed section headers like `[Opening]`, `[Transition]`, `[Closing]` which are then passed to TTS and spoken aloud.

## Goals / Non-Goals

**Goals:**
- Eliminate bracketed section headers from generated briefing scripts
- Make the fix resilient — even if the LLM ignores the prompt, the output is clean

**Non-Goals:**
- Overhauling the entire prompt structure
- Changing TTS behavior or adding TTS-level filtering
- Handling other non-spoken artifacts beyond bracketed headers (e.g., markdown formatting)

## Decisions

### Decision 1: Explicitly name prohibited patterns in the prompt

**Choice:** Add explicit examples of prohibited patterns (`[Opening]`, `[Closing]`, `[Transition]`) to the existing prohibition line in the prompt.

**Rationale:** LLMs respond better to concrete examples than abstract prohibitions. Naming the exact patterns we want to avoid significantly reduces their occurrence. This is the primary fix.

**Alternative considered:** Adding a separate "Format rules" section to the prompt — rejected as over-engineering for a single-line fix.

### Decision 2: Post-generation regex cleanup as safety net

**Choice:** After receiving the LLM response, apply a regex to strip any remaining `[...]` patterns (bracketed text on its own line or at the start of a line) before returning the script.

**Rationale:** LLMs are non-deterministic. Even with a strong prompt, there's no guarantee headers won't appear. A simple regex provides a reliable safety net.

**Pattern:** `^\[.+?]\s*\n` (matches lines that are solely a bracketed header, removes the entire line). This avoids stripping legitimate bracketed content within sentences.

## Risks / Trade-offs

- [Risk] Regex could strip legitimate bracketed content in article text → Mitigation: Pattern only matches lines where bracketed text appears alone on its own line, not inline brackets within sentences
- [Risk] Future LLMs may use different non-spoken markers → Mitigation: The prompt fix is the primary defense; the regex is just a safety net for the known `[...]` pattern

## Context

The drift report (2026-03-03) found 16 discrepancies across 52 specs. After user Q&A, 12 are spec-only updates (code is correct) and 5 are code fixes. This is a sync change — no new architecture, no new dependencies.

## Goals / Non-Goals

**Goals:**
- Bring all 14 affected specs into alignment with the codebase
- Fix 5 code bugs identified in the drift report
- Ensure specs remain the authoritative source of truth

**Non-Goals:**
- No new features or capabilities
- No refactoring beyond the specific fixes
- No database migrations (all fixes are application-level)

## Decisions

### Spec updates are the primary fix direction
Most drift was caused by specs not keeping up with intentional code evolution (DB-driven sources, UI layout changes, surrogate PKs). Updating specs is the correct fix — the code reflects deliberate decisions.

### max-failures default stays at 15
The original spec value of 5 was too aggressive in practice. Both `source-config` and `source-polling-backoff` specs update to 15.

### No Edit Script button on episode detail page
The backend endpoint exists but the frontend button was never needed. Spec removes the requirement rather than adding dead UI.

### Podcast-pipeline manual trigger returns 409
The current HTTP 200 response is semantically wrong — it signals success when the action was rejected. Changing to 409 aligns with REST conventions and enables proper frontend error handling.

### Twitter first-poll userId caching
The fix requires passing the resolved X userId into `buildLastSeenId` so it can be stored even when no tweets are returned. Currently the method only gets the userId from parsing `lastSeenId`, which is null on first poll.

### Ollama default URL includes /v1
Since the project uses the OpenAI-compatible API layer, `/v1` is required in the base URL.

## Risks / Trade-offs

- **409 change is backward-compatible**: Any frontend code already handles non-2xx generically, so the status code change won't break anything. The publish wizard gets explicit 409 handling as a UX improvement.
- **Spec updates have no runtime risk**: They're documentation-only changes.
- **Twitter userId caching fix is low-risk**: Only affects the first poll cycle of a new Twitter source when no tweets exist yet.

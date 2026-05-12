## Context

`LlmPipeline.compose()` dispatches to `BriefingComposer`, `InterviewComposer`, or `DialogueComposer`, each of which builds a fully static prompt and calls `ChatClient.prompt().user(prompt).options(OpenAiChatOptions.builder().model(...).build()).call()`. No temperature is set. Prompts include literal example sentences (e.g. the "Coming up:..." sample in `InterviewComposer.kt`, the cliffhanger samples, the "Stephan, thanks as always" beat) which the LLM has been copying verbatim across daily episodes. The style-differentiating system message is one sentence per style; all engagement instructions (hook opening, front-loading, mid-roll callbacks, sign-off shape) are identical word-for-word across styles.

## Goals / Non-Goals

**Goals:**

- Break the structural sameness so consecutive episodes feel different at the seams.
- Keep regenerations of the *same* episode reproducible (same date + same articles → same scaffolding selection).
- Stay self-contained: no new dependencies, no new infra, no Spring AI tool-calling.

**Non-Goals:**

- Tool-calling, web search, history lookback. Those land in separate changes.
- Rewriting the engagement-technique guidance. Only the verbatim examples are removed; the abstract guidance stays.

## Decisions

### D1. Variety via `PromptVarietyPicker`, not pure temperature

Raising temperature alone keeps the structural beats (the prompt still mandates them). We deterministically rotate three discrete dimensions — opening style, transition vocabulary, sign-off shape — keyed on `hash(podcastId, episodeDate)`. Same episode regenerated → same selection (reproducibility); next day → different selection. Temperature (default 0.95) supplies micro-variation on top.

Alternatives considered:

- Pure temperature bump — keeps structural sameness; rejected.
- Random selection per call — breaks reproducibility for regenerations; rejected.
- LLM-side instruction to "vary the structure" — unreliable across runs; rejected.

### D2. Three rotation dimensions

Initial dimension sets (kept in code, not config):

- **Opening style:** cold-open question / shocking-stat / scene-set / contrarian take / first-person hook.
- **Transition vocabulary:** signposts vocabulary set A / set B / set C.
- **Sign-off shape:** recap-first / forward-look / call-to-action / quote-of-the-day.

`hash(podcastId, episodeDate)` is split across the three dimensions (different byte ranges) so they rotate semi-independently. Three axes are sufficient to break verbatim repetition; can extend later if needed.

### D3. Default temperature 0.95, clamped `[0.0, 2.0]`

`0.95` matches Claude/GPT defaults for creative writing without being so loose it drifts off-topic. Per-podcast `composeTemperature` field overrides. Clamping happens server-side on PUT.

### D4. Strip literal examples, keep abstract guidance

The interruption-style menu in `InterviewComposer` stays (excited, skeptical, confused, connecting dots, playful disagreement) — but the literal example sentences inside each bullet (e.g. `"Wait, wait — did you say 100x?!"`) are removed. Same treatment for the cliffhanger examples and the "Coming up:" example. The abstract instruction ("the interviewer should rattle off 2-3 short punchy fragments previewing the most interesting topics") remains.

### D5. Migration is additive and nullable

`compose_temperature REAL` on `podcasts` is nullable; null means "use the system default". No backfill needed.

## Risks / Trade-offs

- **[Risk] Temperature 0.95 pushes some models off-topic.** → Mitigation: per-podcast override available, regression test on 5 fixture runs verifying no banned-phrase regressions and at least 3 distinct openings.
- **[Risk] Removing literal examples could leave the LLM under-instructed.** → Mitigation: keep the abstract guidance; verify with a manual run that the LLM still produces a "Coming up:" beat in the interview style.
- **[Trade-off] Variety picker rotates only three axes.** → Enough to break verbatim repetition; extend later if needed.
- **[Risk] Hash-based rotation lands on the same selection by collision on a particular date.** → Acceptable; consecutive-day distinctness is asserted in tests, not perfect uniqueness.

## Migration Plan

1. Land the new migration adding `compose_temperature` (additive, nullable, no backfill).
2. Deploy the new composers; existing podcasts continue to work because temperature falls back to the system default.
3. Frontend ships the new input; existing podcasts keep their (null) value until edited.

Rollback: revert code; the new column can stay (no other code depends on it) or be dropped in a follow-up.

## Open Questions

- Should the dimension sets be configurable per podcast? Not in v1; ship with fixed sets and revisit if useful.
- Should `composeTemperature` be exposed in the upcoming-episode preview SSE? Not in v1; settings page is enough.

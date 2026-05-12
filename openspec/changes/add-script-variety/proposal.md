## Why

The last 10 generated episodes read almost identical at the seams: identical hook formula, identical "Welcome to..." opener, identical "Coming up:" teaser shape, identical sign-off, even though the news content varies. Three root causes in the compose stage: (1) prompts are fully static with one-sentence style differentiation, (2) prompts hard-code verbatim example phrases that the LLM copies as scaffolding, and (3) no temperature is passed to the model (`OpenAiChatOptions` sets only `model`, so the model runs at provider defaults but is over-constrained by the prompt).

## What Changes

- Relax the prescriptive, example-laden prompts in `BriefingComposer`, `InterviewComposer`, and `DialogueComposer`. Remove verbatim example phrases (e.g. literal `"Coming up: AI agents going rogue..."`, literal `"Stephan, thanks as always"`, literal cliffhanger samples).
- Pass `temperature` to the compose call. Add a per-podcast `composeTemperature` field with default `0.95`, clamped to `[0.0, 2.0]`.
- Add `PromptVarietyPicker` keyed deterministically on `(podcastId, episodeDate)` that rotates opening style, transition vocabulary, and sign-off shape so day-to-day scaffolding differs. Same episode regenerated yields the same selection.
- Expose `composeTemperature` in the podcast settings UI.

This change is independent of the history-lookback and deep-dive-research changes; it can land in any order.

## Capabilities

### New Capabilities
- `script-variety`: Deterministic per-episode rotation of opening style, transition vocabulary, and sign-off shape, plus the compose-stage temperature control.

### Modified Capabilities
- `llm-processing`: Compose call now passes `temperature` in `OpenAiChatOptions`.
- `dialogue-composition`: Prompt uses the variety picker; verbatim example phrases removed.
- `interview-composition`: Prompt uses the variety picker; verbatim example phrases removed.
- `podcast-customization`: New `composeTemperature` field.
- `frontend-podcast-settings`: UI control for `composeTemperature`.
- `database-migrations`: New migration adding `compose_temperature REAL` to `podcasts`.

## Impact

- **Code** — `src/main/kotlin/com/aisummarypodcast/llm/{BriefingComposer,InterviewComposer,DialogueComposer,ComposerUtils}.kt`; new `llm/PromptVarietyPicker.kt`; updates to `store/Podcast.kt`, request/response mappers.
- **Database** — one new Flyway migration adding `compose_temperature REAL` to `podcasts` (nullable; default treated as `0.95` at read time).
- **API** — `GET/PUT /users/{userId}/podcasts/{podcastId}` gains `composeTemperature`.
- **Frontend** — `frontend/src/app/podcasts/[podcastId]/settings/page.tsx` gains the temperature input.
- **Dependencies** — none.
- **Cost** — no change.

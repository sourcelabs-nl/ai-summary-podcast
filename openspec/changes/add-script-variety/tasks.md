## 1. Database migration

- [ ] 1.1 Write `V<next>__add_podcast_compose_temperature.sql` adding `compose_temperature REAL` to `podcasts`
- [ ] 1.2 Verify the migration applies cleanly against a copy of `./data/ai-summary-podcast.db`

## 2. Domain model + config

- [ ] 2.1 Add `composeTemperature: Double?` to `Podcast` entity, DTO, repository row mapper
- [ ] 2.2 Accept and clamp `composeTemperature` in the podcast create/update endpoints (return 422 on out-of-range)
- [ ] 2.3 Add `app.compose.default-temperature` to `AppProperties` defaulting to `0.95`

## 3. Prompt variety picker

- [ ] 3.1 Implement `PromptVarietyPicker` with rotation across opening style, transition vocabulary, and sign-off shape, keyed on `(podcastId, episodeDate)`
- [ ] 3.2 Unit-test `PromptVarietyPicker`: same key returns same selection; consecutive days produce at least 3 distinct opening selections over 5 days
- [ ] 3.3 Define the three rotation sets as private constants inside the picker (opening styles, transition vocab sets, sign-off shapes)

## 4. Composer refactor

- [ ] 4.1 Refactor `BriefingComposer.buildPrompt` to consume the picker; remove verbatim example phrases
- [ ] 4.2 Refactor `InterviewComposer.buildPrompt` to consume the picker; remove the literal "Coming up:..." example, the literal cliffhanger samples, the literal interruption samples, and the "Stephan, thanks as always" beat
- [ ] 4.3 Refactor `DialogueComposer.buildPrompt` to consume the picker; remove verbatim example phrases
- [ ] 4.4 Wire the resolved `composeTemperature` into `OpenAiChatOptions.builder().temperature(...)` in all three composers
- [ ] 4.5 Update or add tests asserting: temperature is passed; the picker selection is reflected in the prompt; no banned phrases appear in any composer prompt

## 5. Frontend

- [ ] 5.1 Add the `composeTemperature` numeric input to `frontend/src/app/podcasts/[podcastId]/settings/page.tsx` (step 0.05, range 0–2, placeholder "0.95")
- [ ] 5.2 Manual smoke test: edit, save, reload, confirm value persists

## 6. Verification

- [ ] 6.1 Run `mvn test` and ensure all tests pass
- [ ] 6.2 Restart the app via `./stop.sh && ./start.sh`
- [ ] 6.3 Generate 5 episodes back-to-back against the same article fixture; diff the first 200 chars and confirm at least 3 distinct opening styles
- [ ] 6.4 Run `openspec validate add-script-variety --strict`
- [ ] 6.5 Update `README.md` to document the `composeTemperature` setting

## 1. Data Model & Migration

- [x] 1.1 Add `speakerNames` field (`Map<String, String>?`) to `Podcast` entity
- [x] 1.2 Create Flyway migration to add `speaker_names` column (TEXT, nullable) to `podcasts` table
- [x] 1.3 Add `speakerNames` to podcast create/update DTOs and GET response in `PodcastController`

## 2. Validation

- [x] 2.1 Extend `validateTtsConfig()` in `PodcastController` to handle `interview` style: require ElevenLabs provider and exactly `interviewer`/`expert` keys in `ttsVoices`
- [x] 2.2 Add validation tests for interview style (valid config, wrong provider, wrong role keys, missing voices)

## 3. InterviewComposer

- [x] 3.1 Create `InterviewComposer` component with `compose()` method following the same signature as `DialogueComposer`
- [x] 3.2 Write the interview prompt: asymmetric dynamic (interviewer ~20% words, expert ~80%), speaker names support, article summaries, language, continuity context, custom instructions
- [x] 3.3 Add unit tests for `InterviewComposer.buildPrompt()`: verify prompt includes speaker names, handles missing names, includes articles, respects language, includes continuity context

## 4. DialogueComposer Enhancement

- [x] 4.1 Update `DialogueComposer.buildPrompt()` to use `speakerNames` when provided on the podcast
- [x] 4.2 Add unit tests for `DialogueComposer` with speaker names: names appear in prompt, absent names preserve existing behavior

## 5. Pipeline Integration

- [x] 5.1 Update composer selection in `LlmPipeline` from `if/else` to `when` expression routing `interview` → `InterviewComposer`
- [x] 5.2 Add `TtsProviderFactory.resolve()` mapping for `interview` style → `elevenLabsDialogueTtsProvider`
- [x] 5.3 Add unit tests for pipeline routing: interview style uses InterviewComposer, dialogue unchanged, monologue unchanged

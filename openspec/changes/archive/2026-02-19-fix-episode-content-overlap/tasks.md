## 1. Extract shared episode creation logic

- [x] 1.1 Add method to `EpisodeService` that encapsulates post-pipeline logic: save episode, save episode-article links, generate recap, update `lastGeneratedAt`
- [x] 1.2 Refactor `BriefingGenerationScheduler.generateBriefing()` to delegate to the new `EpisodeService` method
- [x] 1.3 Refactor `PodcastController.generate()` to delegate to the new `EpisodeService` method
- [x] 1.4 Update tests for `BriefingGenerationScheduler` and `PodcastController` to verify delegation

## 2. Improve discard article reset

- [x] 2.1 Update `EpisodeController.discard()` to log a warning when no episode-article links are found
- [x] 2.2 Update discard tests to cover both with-links and without-links scenarios

## 3. Add grounding instructions to composer prompts

- [x] 3.1 Add grounding constraint to `BriefingComposer.buildPrompt()`
- [x] 3.2 Add grounding constraint to `DialogueComposer.buildPrompt()`
- [x] 3.3 Add grounding constraint to `InterviewComposer.buildPrompt()`
- [x] 3.4 Update composer tests to verify grounding instruction is present in generated prompts

## 4. Documentation

- [x] 4.1 Add architectural guideline to `CLAUDE.md` requiring controllers to delegate to service/domain logic

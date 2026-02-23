## 1. Utility

- [x] 1.1 Add `extractDomainAndPath(url: String): String` to `ComposerUtils.kt`

## 2. Scheduler and Pipeline — podcast name in logs

- [x] 2.1 Change `BriefingGenerationScheduler.generateBriefing()` to accept `Podcast` instead of `podcastId: String`, update all log lines to include podcast name
- [x] 2.2 Update all log lines in `LlmPipeline.kt` to include podcast name in format `'Name' (uuid)`

## 3. LLM components — podcast name and source context

- [x] 3.1 Add `sourceLabels: Map<String, String>` parameter to `ArticleScoreSummarizer.scoreSummarize()`, build the map in `LlmPipeline`, update article log lines to include source label
- [x] 3.2 Update `BriefingComposer` log lines to include podcast name
- [x] 3.3 Update `DialogueComposer` log lines to include podcast name
- [x] 3.4 Update `InterviewComposer` log lines to include podcast name
- [x] 3.5 Update `EpisodeRecapGenerator` log lines to include podcast name

## 4. TTS and Episode — podcast name in logs

- [x] 4.1 Update `TtsPipeline` log lines to include podcast name
- [x] 4.2 Update `EpisodeService` log lines to include podcast name

## 5. Verification

- [x] 5.1 Run tests to verify no regressions

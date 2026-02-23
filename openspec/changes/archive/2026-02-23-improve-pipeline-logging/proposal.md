## Why

Pipeline log messages reference podcasts by UUID only and articles without their source, making logs hard to follow when debugging or monitoring. Adding human-readable context (podcast name, source domain+path) makes logs immediately understandable without cross-referencing the database.

## What Changes

- All `[Pipeline]`, `[LLM]`, and `[TTS]` log messages that reference a podcast will include the podcast name alongside the UUID (format: `'Name' (uuid)`)
- Article scoring/summarizing log messages will include the source domain+path (e.g., `techcrunch.com/feed`)
- `BriefingGenerationScheduler.generateBriefing()` will accept a `Podcast` instead of `podcastId: String` to avoid a redundant DB fetch for the first log lines
- A source label extraction utility will be added to `ComposerUtils.kt` for deriving `domain/path` from a source URL

## Capabilities

### New Capabilities

_None_

### Modified Capabilities

- `pipeline-observability`: Log messages will include podcast name and article source context for human readability

## Impact

- **Code**: `BriefingGenerationScheduler`, `LlmPipeline`, `ArticleScoreSummarizer`, `BriefingComposer`, `DialogueComposer`, `InterviewComposer`, `EpisodeRecapGenerator`, `TtsPipeline`, `EpisodeService`
- **Signature change**: `ArticleScoreSummarizer.scoreSummarize()` gains a `sourceLabels: Map<String, String>` parameter; `BriefingGenerationScheduler.generateBriefing()` changes from `podcastId: String` to `podcast: Podcast`
- **No API, database, or dependency changes**

## Context

Pipeline log messages currently reference podcasts by UUID and articles without source context. Operators must cross-reference the database to understand which podcast or source a log line refers to. All the data needed for human-readable logs already exists in memory at the call sites — it just isn't being logged.

## Goals / Non-Goals

**Goals:**
- Include podcast name in all `[Pipeline]`, `[LLM]`, and `[TTS]` log messages that reference a podcast
- Include source domain+path in article scoring/summarizing log messages
- Keep UUIDs in log messages for traceability (name supplements, doesn't replace)

**Non-Goals:**
- Structured/JSON logging
- Adding new log statements — only enriching existing ones
- Changing log levels or adding metrics

## Decisions

### Decision 1: Podcast log format — `'Name' (uuid)`

Format: `[Pipeline] Starting briefing generation for podcast 'AI Daily News' (6aa0af72-...)`

**Rationale**: Name first for readability, UUID preserved for grep/traceability. Single quotes around name prevent visual ambiguity with surrounding text.

**Alternative considered**: `uuid (Name)` — less readable since the human-meaningful part comes second.

### Decision 2: Source label extraction — `extractDomainAndPath()` in ComposerUtils

Add a new function `extractDomainAndPath(url: String): String` to `ComposerUtils.kt` that returns `domain/path` (e.g., `techcrunch.com/feed`). The existing `extractDomain()` stays unchanged since it's used in prompt building.

**Rationale**: Reusable utility, consistent with existing pattern of shared functions in `ComposerUtils.kt`.

### Decision 3: Pass source labels map into ArticleScoreSummarizer

`LlmPipeline` already fetches all sources. Build a `Map<String, String>` (sourceId → label) and pass it into `scoreSummarize()` as a parameter.

**Rationale**: Avoids injecting `SourceRepository` into `ArticleScoreSummarizer`. The pipeline orchestrator already has this data.

### Decision 4: Pass `Podcast` to `generateBriefing()` instead of `podcastId`

`BriefingGenerationScheduler.checkAndGenerate()` already iterates over full `Podcast` objects. Passing the `Podcast` to `generateBriefing()` removes the redundant `findById` on line 58 and makes the name available for all log lines.

**Rationale**: Cleaner — eliminates a DB call and makes the method signature more honest about what it needs.

## Risks / Trade-offs

- [Risk: Long podcast names cluttering logs] → Acceptable since names are user-controlled and typically short. No truncation needed.
- [Risk: Source URL parsing edge cases] → `extractDomainAndPath()` falls back to the raw URL on parse failure, same as existing `extractDomain()`.

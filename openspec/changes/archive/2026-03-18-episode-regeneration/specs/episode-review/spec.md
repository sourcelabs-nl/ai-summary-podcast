# Delta: Episode Review

## MODIFIED Requirements

### Requirement: Episode creation from pipeline result
`createEpisodeFromPipelineResult` SHALL accept an optional `overrideGeneratedAt` parameter. When provided, the episode's `generatedAt` field SHALL use this value instead of the current time.

`createEpisodeFromPipelineResult` SHALL accept an optional `updateLastGenerated` parameter (default: `true`). When `false`, the method SHALL skip updating the podcast's `lastGeneratedAt` timestamp.

Existing callers that do not pass these parameters SHALL behave identically to before (defaults preserve current behavior).

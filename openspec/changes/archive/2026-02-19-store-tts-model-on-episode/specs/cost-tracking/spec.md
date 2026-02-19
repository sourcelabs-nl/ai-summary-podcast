## MODIFIED Requirements

### Requirement: Cost data exposed in episode API response
The episode API response SHALL include cost fields: `llmInputTokens`, `llmOutputTokens`, `llmCostCents`, `ttsCharacters`, `ttsCostCents`, and `ttsModel`. All cost and model fields SHALL be nullable (null when data is not available, e.g. for episodes created before cost tracking was added).

#### Scenario: Episode response includes cost data
- **WHEN** an episode is retrieved via `GET /users/{userId}/podcasts/{podcastId}/episodes/{episodeId}`
- **THEN** the response includes `llmInputTokens`, `llmOutputTokens`, `llmCostCents`, `ttsCharacters`, `ttsCostCents`, and `ttsModel` fields

#### Scenario: Legacy episode without cost data
- **WHEN** an episode created before cost tracking is retrieved
- **THEN** all cost fields and `ttsModel` are null

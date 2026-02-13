## 1. Database & Configuration

- [x] 1.1 Create Flyway migration V10 adding `llm_input_tokens`, `llm_output_tokens`, `llm_cost_cents` columns (nullable INTEGER) to `articles` table
- [x] 1.2 In the same V10 migration, add `llm_input_tokens`, `llm_output_tokens`, `llm_cost_cents`, `tts_characters`, `tts_cost_cents` columns (nullable INTEGER) to `episodes` table
- [x] 1.3 Add optional `input-cost-per-mtok` and `output-cost-per-mtok` (Double?) to `ModelDefinition` in config
- [x] 1.4 Add `tts.cost-per-million-chars` (Double?) to `AppProperties`
- [x] 1.5 Update `application.yaml` with example pricing for existing models and TTS

## 2. Entity Updates

- [x] 2.1 Add `llmInputTokens: Int?`, `llmOutputTokens: Int?`, `llmCostCents: Int?` fields to `Article` data class
- [x] 2.2 Add `llmInputTokens: Int?`, `llmOutputTokens: Int?`, `llmCostCents: Int?`, `ttsCharacters: Int?`, `ttsCostCents: Int?` fields to `Episode` data class

## 3. Cost Estimation

- [x] 3.1 Create `CostEstimator` utility class with `estimateLlmCostCents(inputTokens, outputTokens, modelDef)` and `estimateTtsCostCents(characters, costPerMillionChars)` methods
- [x] 3.2 Write unit tests for `CostEstimator` covering configured pricing, missing pricing, and rounding

## 4. LLM Token Extraction

- [x] 4.1 Modify `CachingChatModel` to preserve `Usage` metadata from `ChatResponse` on cache miss (pass through the original response). On cache hit, return a response with zero usage.
- [x] 4.2 Create a `TokenUsage` data class (inputTokens: Int, outputTokens: Int) and a helper to extract it from `ChatResponse.metadata.usage`
- [x] 4.3 Update `RelevanceScorer` to extract token usage after each scoring call and persist `llmInputTokens`, `llmOutputTokens`, `llmCostCents` on the article
- [x] 4.4 Update `ArticleSummarizer` to extract token usage after each summarization call and accumulate token counts and cost on the article
- [x] 4.5 Update `BriefingComposer` to extract token usage from the composition call and return it alongside the script
- [x] 4.6 Extend `PipelineResult` with `llmInputTokens: Int` and `llmOutputTokens: Int` fields

## 5. TTS Character Tracking

- [x] 5.1 Modify `TtsService.generateAudio()` to return a result object containing both the audio chunks and total character count
- [x] 5.2 Update `TtsPipeline` to receive the character count from `TtsService` and store `ttsCharacters` and `ttsCostCents` on the episode

## 6. Pipeline Integration

- [x] 6.1 Update `BriefingGenerationScheduler` to pass `llmInputTokens`, `llmOutputTokens`, and `llmCostCents` from `PipelineResult` to the episode when saving
- [x] 6.2 Update `EpisodeService.generateAudioAsync()` (if applicable) to propagate TTS cost fields

## 7. API Response

- [x] 7.1 Add `llmInputTokens`, `llmOutputTokens`, `llmCostCents`, `ttsCharacters`, `ttsCostCents` fields to `EpisodeResponse` DTO
- [x] 7.2 Update `Episode.toResponse()` mapping to include the new cost fields

## 8. Testing

- [x] 8.1 Write unit tests for token extraction from `ChatResponse` (with and without usage metadata)
- [x] 8.2 Write unit tests for `RelevanceScorer` verifying token counts are persisted on articles
- [x] 8.3 Write unit tests for `ArticleSummarizer` verifying token count accumulation
- [x] 8.4 Write unit tests for `BriefingComposer` verifying token counts are returned in result
- [x] 8.5 Write unit tests for `TtsService` verifying character count is returned
- [x] 8.6 Verify existing tests still pass with new nullable fields

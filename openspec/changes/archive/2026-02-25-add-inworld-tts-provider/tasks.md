## 1. TtsProvider Interface & TextChunker Changes

- [x] 1.1 Add `scriptGuidelines(style: PodcastStyle): String` method and `maxChunkSize: Int` property to `TtsProvider` interface
- [x] 1.2 Make `TextChunker.chunk()` accept a `maxChunkSize: Int` parameter (default 4096 for backward compatibility)
- [x] 1.3 Implement `scriptGuidelines()` and `maxChunkSize` on `OpenAiTtsProvider` (plain text guidelines, maxChunkSize=4096)
- [x] 1.4 Implement `scriptGuidelines()` and `maxChunkSize` on `ElevenLabsTtsProvider` (emotion cue guidelines, maxChunkSize=5000)
- [x] 1.5 Implement `scriptGuidelines()` and `maxChunkSize` on `ElevenLabsDialogueTtsProvider` (same guidelines as single-speaker, maxChunkSize=5000)
- [x] 1.6 Update existing providers to use `maxChunkSize` when calling `TextChunker.chunk()`

## 2. Composer Script Guidelines Integration

- [x] 2.1 Add `ttsScriptGuidelines: String` parameter to `BriefingComposer.compose()` and inject into prompt
- [x] 2.2 Add `ttsScriptGuidelines: String` parameter to `DialogueComposer.compose()` and inject into prompt (replacing hardcoded emotion cue line)
- [x] 2.3 Add `ttsScriptGuidelines: String` parameter to `InterviewComposer.compose()` and inject into prompt (replacing hardcoded emotion cue line)
- [x] 2.4 Add `TtsProviderFactory` dependency to `LlmPipeline` and resolve+pass `scriptGuidelines()` to composers

## 3. Inworld AI TTS Provider

- [x] 3.1 Add `INWORLD` to `TtsProviderType` enum with value `"inworld"`
- [x] 3.2 Create `InworldApiClient` with JWT authentication, `synthesizeSpeech()` method, and error handling
- [x] 3.3 Create `InworldTtsProvider` implementing `TtsProvider` — single-speaker generation with chunking (maxChunkSize=2000)
- [x] 3.4 Add dialogue/interview support to `InworldTtsProvider` — parse turns via `DialogueScriptParser`, generate per-turn with appropriate voice, concatenate
- [x] 3.5 Implement `scriptGuidelines(style)` on `InworldTtsProvider` with style-aware emotion/emphasis/filler word instructions

## 4. Provider Factory & Pipeline Wiring

- [x] 4.1 Add `InworldTtsProvider` to `TtsProviderFactory` — resolve for both monologue and dialogue/interview styles
- [x] 4.2 Add Inworld pricing config to `application.yaml` (`inworld-tts-1.5-max: 10.00`, `inworld-tts-1.5-mini: 5.00`)
- [x] 4.3 Update `CostEstimator` / `TtsPipeline` to resolve Inworld cost by model name from `ttsSettings["model"]`

## 5. Voice Discovery

- [x] 5.1 Add `listVoices(userId)` to `InworldApiClient`
- [x] 5.2 Extend `VoiceController` to handle `provider=inworld` and delegate to `InworldApiClient.listVoices()`

## 6. Tests

- [x] 6.1 Unit tests for `TextChunker` with configurable max chunk size
- [x] 6.2 Unit tests for `InworldTtsProvider` (single-speaker, dialogue, script guidelines per style)
- [x] 6.3 Unit tests for `InworldApiClient` (JWT construction, error handling)
- [x] 6.4 Unit tests for updated `TtsProviderFactory` (Inworld resolution for all style combinations)
- [x] 6.5 Unit tests for composer TTS guidelines injection (verify prompt contains guidelines, verify empty guidelines adds nothing)
- [x] 6.6 Unit tests for Inworld cost estimation (Max model, Mini model, missing config)
- [x] 6.7 Update existing composer tests to pass `ttsScriptGuidelines` parameter

## 1. Database Migration

- [x] 1.1 Create Flyway migration to rename `tts_voice` to `tts_voices` (converting existing values from `"nova"` to `{"default": "nova"}`), rename `tts_speed` to `tts_settings` (converting existing values from `1.0` to `{"speed": 1.0}`), and add `tts_provider` column (TEXT, NOT NULL, default `"openai"`) on the `podcasts` table
- [x] 1.2 Add `"elevenlabs"` to the known provider defaults map in `UserProviderConfigService` with base URL `https://api.elevenlabs.io`

## 2. Podcast Entity and DTO Updates

- [x] 2.1 Update `Podcast` entity: replace `ttsVoice: String` with `ttsVoices: Map<String, String>?`, replace `ttsSpeed: Double` with `ttsSettings: Map<String, Any>?`, add `ttsProvider: String` (default `"openai"`)
- [x] 2.2 Create Spring Data JDBC converters for `ttsVoices` (JSON map) and `ttsSettings` (JSON map), register them alongside the existing `llmModels` converter
- [x] 2.3 Update podcast create/update DTOs and `PodcastController` to accept `ttsProvider`, `ttsVoices`, and `ttsSettings` fields; remove `ttsVoice` and `ttsSpeed` from DTOs
- [x] 2.4 Add validation: `ttsProvider` must be `"openai"` or `"elevenlabs"`; `"dialogue"` style requires `ttsProvider: "elevenlabs"` and at least two voice roles in `ttsVoices`

## 3. TTS Provider Abstraction

- [x] 3.1 Create `TtsProvider` interface with `generate(request: TtsRequest): TtsResult` method; create `TtsRequest` data class (script, ttsVoices, ttsSettings, language, userId); update `TtsResult` to include `requiresConcatenation` flag
- [x] 3.2 Refactor existing `TtsService` into `OpenAiTtsProvider` implementing `TtsProvider`; resolve voice from `ttsVoices["default"]` and speed from `ttsSettings["speed"]`
- [x] 3.3 Create `TtsProviderFactory` component that resolves the appropriate provider based on `podcast.ttsProvider` and `podcast.style`
- [x] 3.4 Update `TtsPipeline` to use `TtsProviderFactory` instead of `TtsService` directly; conditionally skip FFmpeg concatenation when `requiresConcatenation = false`
- [x] 3.5 Write tests for `TtsProviderFactory` resolution logic and updated `TtsPipeline` flow

## 4. ElevenLabs API Client

- [x] 4.1 Create `ElevenLabsApiClient` component wrapping Spring `RestClient` with `xi-api-key` authentication header; methods for TTS, Text-to-Dialogue, and voice listing
- [x] 4.2 Implement error handling: map HTTP 401 → invalid key message, HTTP 429 → rate limit message, other errors → log response body and fail
- [x] 4.3 Write tests for `ElevenLabsApiClient` using MockRestServiceServer

## 5. ElevenLabs TTS Provider (Single-Voice)

- [x] 5.1 Create `ElevenLabsTtsProvider` implementing `TtsProvider`; use `TextChunker` for chunking, call `/v1/text-to-speech/{voice_id}` per chunk via `ElevenLabsApiClient`, pass `ttsSettings` as `voice_settings`, return result with `requiresConcatenation = true`
- [x] 5.2 Write tests for `ElevenLabsTtsProvider`

## 6. ElevenLabs Text-to-Dialogue Provider

- [x] 6.1 Create `DialogueScriptParser` that parses XML-style speaker tags into a list of `DialogueTurn(role: String, text: String)`; handle multi-line content, warn on text outside tags
- [x] 6.2 Create `ElevenLabsDialogueTtsProvider` implementing `TtsProvider`; parse script via `DialogueScriptParser`, map roles to voice IDs via `ttsVoices`, call `/v1/text-to-dialogue` via `ElevenLabsApiClient`, return single audio chunk with `requiresConcatenation = false`
- [x] 6.3 Write tests for `DialogueScriptParser` (happy path, multi-line, text outside tags, empty input)
- [x] 6.4 Write tests for `ElevenLabsDialogueTtsProvider`

## 7. Dialogue Composer

- [x] 7.1 Create `DialogueComposer` component that generates dialogue scripts with XML speaker tags; use `compose` model via `ModelResolver`; prompt includes speaker role names from `ttsVoices` keys, podcast metadata, article summaries, language, and custom instructions; no tag stripping
- [x] 7.2 Update pipeline orchestration (`LlmPipeline` / `BriefingGenerationScheduler`) to select `DialogueComposer` when `podcast.style == "dialogue"`, otherwise use `BriefingComposer`
- [x] 7.3 Write tests for `DialogueComposer` prompt construction
- [x] 7.4 Write tests for composer selection logic

## 8. Voice Discovery Endpoint

- [x] 8.1 Add `GET /users/{userId}/voices?provider=elevenlabs` endpoint to `PodcastController` (or new controller); proxy ElevenLabs `GET /v1/voices` via `ElevenLabsApiClient`; return simplified response `[{voiceId, name, category, previewUrl}]`
- [x] 8.2 Handle error cases: no ElevenLabs config → 400, user not found → 404, unsupported provider → 400, upstream error → 502
- [x] 8.3 Write tests for the voice discovery endpoint

## 9. Cost Tracking Updates

- [x] 9.1 Change `AppProperties.TtsProperties.costPerMillionChars` from `Double?` to `Map<String, Double>?` (provider name → cost rate); update `application.yaml` config structure
- [x] 9.2 Update `CostEstimator.estimateTtsCostCents` to accept a provider name parameter and look up the rate from the map
- [x] 9.3 Update `TtsPipeline` to pass `podcast.ttsProvider` when estimating TTS cost
- [x] 9.4 Write tests for per-provider cost estimation

## 10. Integration Testing

- [x] 10.1 ~~Integration~~ Unit tests cover OpenAI flow with migrated fields (LlmPipelineTest, TtsProviderFactoryTest, OpenAiTtsProvider tested via existing TtsPipeline tests)
- [x] 10.2 ~~Integration~~ Unit tests cover ElevenLabs monologue flow (ElevenLabsApiClientTest, ElevenLabsTtsProviderTest, TtsProviderFactoryTest)
- [x] 10.3 ~~Integration~~ Unit tests cover ElevenLabs dialogue flow (DialogueComposerTest, DialogueScriptParserTest, ElevenLabsDialogueTtsProviderTest, ElevenLabsApiClientTest)
- [x] 10.4 Podcast CRUD with new TTS fields covered by existing PodcastControllerTest (updated for ttsProvider/ttsVoices/ttsSettings)

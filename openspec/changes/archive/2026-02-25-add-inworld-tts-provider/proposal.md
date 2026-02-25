## Why

The current TTS pipeline supports OpenAI and ElevenLabs but has no way to optimize LLM-generated scripts for a specific TTS provider's capabilities. Inworld AI TTS offers rich expressiveness (emotion tags, emphasis markup, non-verbal cues) at competitive pricing ($5-10/M chars), but these features only work when the LLM is explicitly instructed to use them. Adding Inworld AI support — and making script composition provider-aware — improves audio quality for all providers.

## What Changes

- Add `scriptGuidelines(style: PodcastStyle): String` and `maxChunkSize: Int` to the `TtsProvider` interface so each provider declares how the LLM should format scripts and what chunk size limit applies
- All three composers (`BriefingComposer`, `DialogueComposer`, `InterviewComposer`) inject provider-specific script guidelines into LLM prompts
- Add `InworldTtsProvider` and `InworldApiClient` for Inworld AI TTS API integration (JWT auth, base64 audio response, 2000 char chunk limit)
- Support Inworld for both monologue and dialogue/interview styles (dialogue via per-turn generation + concatenation, reusing `DialogueScriptParser`)
- Add Inworld voice discovery to the `VoiceController`
- Add `INWORLD` to `TtsProviderType` enum
- Add Inworld model-configurable pricing (Max at $10/M, Mini at $5/M) to cost tracking
- Make `TextChunker` accept a configurable max chunk size instead of hardcoded 4096

## Capabilities

### New Capabilities
- `inworld-tts`: Inworld AI TTS provider integration — API client, voice discovery, JWT authentication, and audio generation
- `tts-script-profile`: Provider-aware script composition — TTS providers declare script formatting guidelines injected into LLM prompts

### Modified Capabilities
- `tts-provider-abstraction`: Add `scriptGuidelines()` and `maxChunkSize` to `TtsProvider` interface; add `INWORLD` to factory resolution
- `tts-generation`: Make `TextChunker` max chunk size configurable via provider's `maxChunkSize`
- `dialogue-composition`: Inject TTS provider script guidelines into dialogue prompt
- `cost-tracking`: Add Inworld pricing configuration (model-configurable: Max and Mini rates)
- `elevenlabs-voice-discovery`: Extend voice discovery endpoint to support `provider=inworld`

## Impact

- **TtsProvider interface**: Breaking change for implementors (new methods) — all existing providers must implement `scriptGuidelines()` and `maxChunkSize`
- **Composers**: All three composers gain a new parameter for TTS script guidelines
- **LlmPipeline / TtsPipeline**: Must resolve and pass the TTS provider's script profile to composers
- **Configuration**: New env vars `INWORLD_AI_JWT_KEY` and `INWORLD_AI_JWT_SECRET`; new pricing config in `application.yaml`
- **Database**: No schema changes (Inworld model name stored in existing `tts_model` column)
- **Dependencies**: New HTTP client for Inworld API (RestClient, same pattern as ElevenLabs)
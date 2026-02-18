## Why

The TTS system is tightly coupled to OpenAI — there is no provider abstraction and no way to use alternative TTS services. ElevenLabs offers superior voice quality and a unique Text-to-Dialogue capability that can generate natural multi-speaker conversations, which is ideal for a podcast format. Adding ElevenLabs support enables both higher-quality single-voice narration and a new dialogue-style podcast format with multiple speakers.

## What Changes

- Introduce a TTS provider abstraction (`TtsProvider` interface) to decouple the pipeline from any specific TTS service
- Implement an ElevenLabs TTS provider for single-voice audio generation via the `/v1/text-to-speech/{voice_id}` API
- Implement an ElevenLabs Text-to-Dialogue provider for multi-speaker audio generation via the `/v1/text-to-dialogue` API
- Refactor the existing OpenAI TTS logic into a dedicated `OpenAiTtsProvider` behind the new abstraction
- Add a `DialogueComposer` for generating dialogue scripts with XML-style speaker tags (`<host>`, `<cohost>`)
- **BREAKING**: Replace `tts_voice` (TEXT) and `tts_speed` (REAL) fields on `Podcast` with `tts_voices` (JSON map, e.g. `{"default": "nova"}`) and `tts_settings` (JSON map for provider-specific settings like speed, stability)
- Add `tts_provider` field to `Podcast` (`"openai"` or `"elevenlabs"`)
- Add `"dialogue"` as a new podcast style that triggers the `DialogueComposer` and multi-speaker TTS
- Add `"elevenlabs"` as a known TTS provider in `UserProviderConfig` with default base URL `https://api.elevenlabs.io`
- Support multiple TTS provider configs per user (e.g., OpenAI + ElevenLabs simultaneously)
- Add a voice discovery proxy endpoint for browsing the ElevenLabs voice library
- Per-provider TTS cost rates instead of a single global `costPerMillionChars`

## Capabilities

### New Capabilities
- `elevenlabs-tts`: ElevenLabs Text-to-Speech integration for single-voice and multi-speaker dialogue audio generation
- `tts-provider-abstraction`: Provider-agnostic TTS interface allowing pluggable TTS backends
- `dialogue-composition`: LLM-driven dialogue script generation with speaker-tagged output for multi-voice podcasts
- `elevenlabs-voice-discovery`: Proxy endpoint for browsing and searching the ElevenLabs voice library

### Modified Capabilities
- `tts-generation`: Refactor to use the new `TtsProvider` abstraction instead of direct OpenAI coupling; chunking and concatenation become provider-specific concerns
- `podcast-customization`: Replace `tts_voice`/`tts_speed` with `tts_voices` map and `tts_settings` map; add `tts_provider` field; add `"dialogue"` style
- `user-api-keys`: Add `"elevenlabs"` as a known provider with default base URL; support multiple providers per TTS category
- `cost-tracking`: Per-provider TTS cost rates instead of a single global rate

## Impact

- **Database**: Migration to rename `tts_voice`→`tts_voices` (JSON), `tts_speed`→`tts_settings` (JSON), add `tts_provider` column on `podcasts` table
- **TTS layer**: New `TtsProvider` interface, `OpenAiTtsProvider`, `ElevenLabsTtsProvider`, `ElevenLabsDialogueTtsProvider` implementations
- **LLM layer**: New `DialogueComposer` alongside existing `BriefingComposer`; script format parsing for dialogue turns
- **API**: New voice discovery endpoint; updated podcast CRUD DTOs for new fields
- **Dependencies**: ElevenLabs REST API client (manual HTTP via `RestClient` or similar — no Spring AI integration needed)
- **Config**: New known provider default URL, per-provider cost configuration

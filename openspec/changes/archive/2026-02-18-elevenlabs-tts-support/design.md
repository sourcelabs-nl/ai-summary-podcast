## Context

The TTS system is currently hardwired to OpenAI via Spring AI's `OpenAiAudioSpeechModel`. The `TtsService` directly instantiates OpenAI-specific classes, and the `TtsPipeline` assumes a chunk-based workflow (4096 char chunks → N API calls → FFmpeg concatenation). There is no provider abstraction.

ElevenLabs offers two relevant APIs:
- **Text-to-Speech** (`POST /v1/text-to-speech/{voice_id}`) — single-voice, similar to OpenAI TTS
- **Text-to-Dialogue** (`POST /v1/text-to-dialogue`) — multi-speaker, accepts an array of `{text, voice_id}` inputs and returns a single audio file

The LLM composition step currently produces a monologue script and strips any bracketed tags. For dialogue, a separate `DialogueComposer` must produce XML-tagged speaker turns (`<host>`, `<cohost>`).

## Goals / Non-Goals

**Goals:**
- Introduce a `TtsProvider` interface that decouples TTS generation from any specific service
- Implement ElevenLabs TTS (single-voice) and Text-to-Dialogue (multi-speaker) providers
- Refactor existing OpenAI TTS into the provider abstraction
- Add a `DialogueComposer` for generating multi-speaker scripts
- Replace `ttsVoice`/`ttsSpeed` with `ttsVoices` (map) and `ttsSettings` (map) on Podcast
- Add voice discovery endpoint proxying ElevenLabs voice library
- Support per-provider TTS cost rates

**Non-Goals:**
- Real-time/streaming TTS
- Voice cloning or custom voice creation via our API
- Other TTS providers (Google Cloud TTS, Azure, etc.) — the abstraction enables them but we don't implement them now
- ElevenLabs sound effects or music generation
- Migrating existing podcasts automatically to the new field format (manual migration via API)

## Decisions

### 1. TTS Provider Interface

**Decision**: Introduce a `TtsProvider` interface with a single method:

```kotlin
interface TtsProvider {
    fun generate(request: TtsRequest): TtsResult
}
```

Where `TtsRequest` encapsulates the script text, voice configuration, and provider-specific settings. The `TtsPipeline` resolves the appropriate provider based on `podcast.ttsProvider` and delegates to it.

**Why**: A simple interface keeps the abstraction thin. Each provider handles its own chunking, API calls, and audio assembly internally. The pipeline doesn't need to know about chunk sizes or concatenation strategies.

**Alternatives considered**:
- Strategy pattern with a factory: More complex, unnecessary since provider selection is per-podcast and resolved once per generation
- Spring AI abstraction: Spring AI doesn't support ElevenLabs or multi-speaker dialogue

### 2. Provider Resolution

**Decision**: A `TtsProviderFactory` component resolves the provider based on `podcast.ttsProvider` and `podcast.style`:

| ttsProvider | style | Provider |
|-------------|-------|----------|
| `openai` | any | `OpenAiTtsProvider` |
| `elevenlabs` | `dialogue` | `ElevenLabsDialogueTtsProvider` |
| `elevenlabs` | any other | `ElevenLabsTtsProvider` |

**Why**: The `ttsProvider` field selects the service, the `style` field determines the mode. This keeps the podcast model clean — no need for a separate "tts mode" field.

### 3. Voice Configuration via `ttsVoices` Map

**Decision**: Replace `ttsVoice: String` with `ttsVoices: Map<String, String>?` stored as JSON. For monologue: `{"default": "nova"}`. For dialogue: `{"host": "voice_id", "cohost": "voice_id"}`.

**Why**: Consistent with the existing `llmModels` pattern. The `"default"` key is used by monologue providers. Dialogue providers look up role keys that match the XML tags in the script.

**Alternatives considered**:
- Separate `ttsVoiceHost`/`ttsVoiceCohost` fields: Inflexible, doesn't scale to more speakers
- Keep `ttsVoice` alongside `ttsVoices`: Redundant, two fields for the same concern

### 4. Provider-Specific Settings via `ttsSettings` Map

**Decision**: Replace `ttsSpeed: Double` with `ttsSettings: Map<String, Any>?` stored as JSON. Examples:
- OpenAI: `{"speed": 1.25}`
- ElevenLabs: `{"stability": 0.5, "similarity_boost": 0.8}`

**Why**: Each provider has different tuning knobs. A typed field per setting doesn't scale. A JSON map is flexible and consistent with the `ttsVoices`/`llmModels` pattern.

### 5. Dialogue Script Format

**Decision**: The `DialogueComposer` instructs the LLM to output XML-style speaker tags:

```
<host>Welcome back to the show! Big news today.</host>
<cohost>Yeah, Apple just announced their new chip.</cohost>
<host>What do you make of that?</host>
```

The ElevenLabs dialogue provider parses these tags into the `inputs` array format required by the API: `[{text, voice_id}, ...]`. Tag names map to keys in `ttsVoices`.

**Why**: XML tags are robust to parse, LLMs generate them reliably, and they naturally support multiple speakers. The tag names directly correspond to voice mapping keys.

**Alternatives considered**:
- `HOST: "text"` markdown labels: Ambiguous with colons in dialogue
- JSON array output: LLMs sometimes produce malformed JSON for longer outputs

### 6. DialogueComposer as a Separate Component

**Decision**: Create `DialogueComposer` alongside `BriefingComposer`, selected based on podcast style. Both share the `compose` model resolution but differ in prompt construction and post-processing.

**Why**: The dialogue prompt is fundamentally different — it instructs two speakers to converse rather than a single narrator to monologue. Separate classes avoid conditional complexity in the existing `BriefingComposer` and keep each composer focused.

### 7. ElevenLabs HTTP Client

**Decision**: Use Spring's `RestClient` to call ElevenLabs APIs directly. No Spring AI integration — ElevenLabs is not supported by Spring AI.

**Why**: The ElevenLabs API is straightforward REST. A thin client class (`ElevenLabsApiClient`) wrapping `RestClient` is sufficient. Authentication via `xi-api-key` header from the user's provider config.

### 8. Text-to-Dialogue Skips Chunking and FFmpeg

**Decision**: The dialogue provider sends the full parsed dialogue to a single API call and receives a single audio file. No chunking, no FFmpeg concatenation.

**Why**: The Text-to-Dialogue API handles multi-speaker mixing internally and returns one audio file. Chunking would break the conversation flow.

**Risk**: Very long dialogues may exceed API limits. Mitigation: enforce a reasonable target word count for dialogue scripts and handle API errors gracefully.

### 9. ElevenLabs TTS (Monologue) Chunking

**Decision**: The ElevenLabs single-voice provider uses the existing `TextChunker` with a configurable max chunk size (ElevenLabs limit TBD, likely similar to OpenAI). Audio chunks are concatenated via FFmpeg, same as OpenAI.

**Why**: The monologue flow is structurally identical to OpenAI — only the API client differs.

### 10. Voice Discovery Endpoint

**Decision**: Add `GET /users/{userId}/voices?provider=elevenlabs` that proxies the ElevenLabs voices API (`GET /v1/voices`). Returns a simplified list of `{voiceId, name, category, previewUrl}`.

**Why**: Users need to discover ElevenLabs voice IDs to configure their podcasts. Proxying avoids exposing the user's API key to the frontend and allows us to shape the response.

### 11. Per-Provider TTS Cost Rates

**Decision**: Change `app.tts.cost-per-million-chars` to a per-provider map in config:

```yaml
app:
  tts:
    cost-per-million-chars:
      openai: 15.00
      elevenlabs: 30.00
```

The cost estimator looks up the rate by `podcast.ttsProvider`.

**Why**: ElevenLabs and OpenAI have different pricing. A single global rate is inaccurate.

## Risks / Trade-offs

- **ElevenLabs API limits unknown** → Mitigation: implement sensible defaults, handle 413/429 errors gracefully, log clearly
- **Dialogue script parsing brittleness** → Mitigation: use a lenient XML parser, handle unmatched tags gracefully (treat as narration), log warnings
- **Breaking change on `ttsVoice`/`ttsSpeed`** → Mitigation: DB migration converts existing data (`ttsVoice: "nova"` → `ttsVoices: {"default": "nova"}`, `ttsSpeed: 1.0` → `ttsSettings: {"speed": 1.0}`)
- **LLM quality for dialogue scripts** → Mitigation: use the `compose` model (capable model) with clear prompt engineering; the `DialogueComposer` can include example output in the prompt
- **Cost of ElevenLabs** → Mitigation: cost estimation with per-provider rates, visible in episode API response

## Migration Plan

1. DB migration: rename `tts_voice` → `tts_voices` (convert existing values to JSON map `{"default": "<value>"}`), rename `tts_speed` → `tts_settings` (convert to `{"speed": <value>}`), add `tts_provider` column (default `"openai"`)
2. Update `Podcast` entity, DTOs, and CRUD endpoints
3. Deploy — existing podcasts continue working with OpenAI provider and migrated voice/settings
4. Users can then configure ElevenLabs provider and create dialogue-style podcasts

## Open Questions

- What is the ElevenLabs character limit per Text-to-Dialogue API call? Need to test or find in docs.
- Should the voice discovery endpoint support pagination/search, or just return all voices?

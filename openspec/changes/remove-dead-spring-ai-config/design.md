## Context

The application uses Spring AI for LLM (OpenRouter) and TTS (OpenAI) calls. When per-user API keys were introduced, `ChatClientFactory` and `TtsService` were rewritten to construct their own `OpenAiChatModel` and `OpenAiAudioSpeechModel` instances using keys resolved via `UserApiKeyService`. This made the global Spring AI auto-configuration dead code:

- `AiConfig.kt` defines a `chatClient` bean that nothing injects
- `application.yaml` has a `spring.ai` block that drives auto-configured beans nobody uses
- The auto-configuration still runs at startup, creating unused beans and requiring the env vars to be set for Spring AI's property binding (even though the actual API calls use `System.getenv()` via `UserApiKeyService`)

## Goals / Non-Goals

**Goals:**
- Remove dead configuration so the only API key path is `UserApiKeyService.resolveKey()`
- Prevent Spring AI auto-configuration from creating unused beans
- Keep the `OPENROUTER_API_KEY` / `OPENAI_API_KEY` env var fallback in `UserApiKeyService` working

**Non-Goals:**
- Changing how `ChatClientFactory` or `TtsService` construct their models
- Removing the env var fallback from `UserApiKeyService`
- Removing the Spring AI dependency itself (still needed for `OpenAiChatModel`, `OpenAiAudioSpeechModel`, `ChatClient`, etc.)

## Decisions

### 1. Exclude Spring AI auto-configuration via `spring.autoconfigure.exclude`

**Choice:** Add `spring.autoconfigure.exclude` entries in `application.yaml` rather than using `@SpringBootApplication(exclude = ...)`.

**Rationale:** Keeping exclusions in `application.yaml` makes them visible alongside the rest of the config and avoids modifying the main application class. The auto-configuration classes to exclude are `OpenAiAutoConfiguration` (chat) and `OpenAiAudioSpeechAutoConfiguration` (TTS).

**Alternative considered:** Using `@SpringBootApplication(exclude = [...])` — works equally well but buries the exclusions in code rather than config.

### 2. Remove `spring.ai` block entirely from `application.yaml`

**Choice:** Remove the whole `spring.ai` block rather than leaving it commented out.

**Rationale:** With auto-configuration excluded, the properties serve no purpose. Leaving them commented out adds noise. The base URLs and model names are already hardcoded in `ChatClientFactory` and `TtsService` respectively — those are the source of truth.

### 3. Delete `AiConfig.kt`

**Choice:** Delete the file entirely rather than emptying the class.

**Rationale:** The class has a single bean method that nothing uses. An empty `@Configuration` class serves no purpose.

## Risks / Trade-offs

- **Risk: Wrong auto-configuration class names** → Verify the exact class names from the Spring AI dependency before implementation. If the class names are wrong, the app will still start but create unused beans silently.
- **Risk: Spring AI auto-config creates beans needed transitively** → Mitigated by the fact that `ChatClientFactory` and `TtsService` construct all Spring AI objects manually. No other code injects auto-configured Spring AI beans.

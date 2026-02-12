## Why

The per-user API key feature (`UserApiKeyService`, `ChatClientFactory`, per-user `TtsService`) fully replaced the global Spring AI auto-configuration for both LLM and TTS calls. The global `spring.ai` config block in `application.yaml` and the `AiConfig` bean are now dead code — Spring AI creates auto-configured beans from them, but nothing injects or uses those beans. This is confusing for maintainers who can't tell which config path is actually active.

## What Changes

- Remove the `AiConfig` class (`config/AiConfig.kt`) — its `chatClient` bean is never injected anywhere
- Remove the `spring.ai` block from `application.yaml` — the auto-configured OpenAI chat and audio beans it drives are unused
- Keep the `OPENROUTER_API_KEY` and `OPENAI_API_KEY` environment variables — they are still used as fallback in `UserApiKeyService.globalKeyForProvider()`, but accessed via `System.getenv()`, not through Spring AI properties
- Disable Spring AI auto-configuration to prevent startup errors from missing properties

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `user-api-keys`: The fallback to global env vars becomes the only key resolution path when no per-user key is configured, rather than coexisting with an unused Spring AI auto-config. No behavioral change, but the configuration surface is reduced.

## Impact

- **Code**: Remove `AiConfig.kt`. Modify `application.yaml`.
- **Startup**: Spring AI auto-configuration must be excluded to prevent it from failing on missing `spring.ai.openai.api-key`. This can be done via `@SpringBootApplication(exclude = ...)` or `spring.autoconfigure.exclude` in `application.yaml`.
- **Environment**: `OPENROUTER_API_KEY` and `OPENAI_API_KEY` env vars are still required (used by `UserApiKeyService` fallback) — no change for operators.
- **Risk**: Low. No runtime behavior changes — only removing unused beans and config.

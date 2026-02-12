## 1. Remove dead code

- [x] 1.1 Delete `src/main/kotlin/com/aisummarypodcast/config/AiConfig.kt`
- [x] 1.2 Remove the `spring.ai` block from `src/main/resources/application.yaml`

## 2. Exclude Spring AI auto-configuration

- [x] 2.1 Look up the exact auto-configuration class names from the Spring AI dependency on the classpath
- [x] 2.2 Add `spring.autoconfigure.exclude` entries in `application.yaml` for the chat and audio speech auto-configuration classes

## 3. Verify

- [x] 3.1 Start the application and confirm it boots without errors
- [x] 3.2 Confirm no Spring AI auto-configured beans are created (no `chatClient` or `speechModel` beans in context)
- [ ] 3.3 Trigger a `/generate` call and confirm the pipeline still works end-to-end using `UserApiKeyService` key resolution

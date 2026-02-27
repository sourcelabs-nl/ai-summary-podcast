## 1. Script Post-Processor

- [x] 1.1 Create `InworldScriptPostProcessor` object in `src/main/kotlin/com/aisummarypodcast/tts/` with `process(script: String): String` method implementing all six transformation steps (double asterisks → single, strip markdown headers, strip markdown bullets, convert markdown links, strip emojis, whitelist non-verbal tags)
- [x] 1.2 Write unit tests for `InworldScriptPostProcessor` covering each transformation step individually and a combined scenario

## 2. Script Guidelines Enhancement

- [x] 2.1 Expand `CORE_GUIDELINES` in `InworldTtsProvider` with text normalization rules, anti-markdown/double-asterisk warning, contractions guidance, and sentence-end punctuation rule
- [x] 2.2 Update `InworldTtsProvider` tests to verify the expanded guidelines content

## 3. API Parameter and Default Temperature

- [x] 3.1 Add `applyTextNormalization = true` to the request body in `InworldApiClient.synthesizeSpeech()`
- [x] 3.2 Change `InworldTtsProvider` to default temperature to `0.8` when not explicitly configured in `ttsSettings`
- [x] 3.3 Update `InworldApiClient` and `InworldTtsProvider` tests for the new API parameter and default temperature

## 4. Post-Processing Integration

- [x] 4.1 Integrate `InworldScriptPostProcessor.process()` into `InworldTtsProvider.generateMonologue()` before chunking
- [x] 4.2 Integrate `InworldScriptPostProcessor.process()` into `InworldTtsProvider.generateDialogue()` for each turn's text before chunking
- [x] 4.3 Add integration tests verifying post-processing is applied in both monologue and dialogue paths

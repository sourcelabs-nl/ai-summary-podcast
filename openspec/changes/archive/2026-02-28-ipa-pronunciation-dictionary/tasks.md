## Tasks

### 1. Database & Entity

- [x] 1.1 Add Flyway migration to add `pronunciations TEXT` column to `podcasts` table
- [x] 1.2 Add `pronunciations: Map<String, String>? = null` field to `Podcast` entity

### 2. API Layer

- [x] 2.1 Add `pronunciations` field to `CreatePodcastRequest`, `UpdatePodcastRequest` DTOs
- [x] 2.2 Add `pronunciations` field to `PodcastResponse` DTO
- [x] 2.3 Pass `pronunciations` through in `PodcastController` create/update mappings
- [x] 2.4 Propagate `pronunciations` in `PodcastService.create()` and `PodcastService.update()`

### 3. TTS Provider Interface

- [x] 3.1 Extend `TtsProvider.scriptGuidelines()` signature to accept `pronunciations: Map<String, String> = emptyMap()`
- [x] 3.2 Update `OpenAiTtsProvider`, `ElevenLabsTtsProvider`, `ElevenLabsDialogueTtsProvider` signatures (ignore pronunciations param)

### 4. Inworld Integration

- [x] 4.1 Update `InworldTtsProvider.scriptGuidelines()` to accept and use pronunciations parameter — append "Pronunciation Guide" section when non-empty
- [x] 4.2 Update `LlmPipeline` to pass `podcast.pronunciations ?: emptyMap()` to `scriptGuidelines()`

### 5. Tests

- [x] 5.1 Add tests for `InworldTtsProvider.scriptGuidelines()` with pronunciations (non-empty and empty map)
- [x] 5.2 Update `LlmPipelineTest` to verify pronunciations are passed through
- [x] 5.3 Add controller/integration test for pronunciations in create/update/get

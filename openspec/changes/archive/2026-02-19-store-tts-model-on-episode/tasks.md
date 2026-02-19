## 1. Database

- [x] 1.1 Add Flyway migration `V28__add_episode_tts_model.sql` with `ALTER TABLE episodes ADD COLUMN tts_model TEXT`

## 2. Domain model

- [x] 2.1 Add `ttsModel: String? = null` field to the `Episode` data class in `store/Episode.kt`

## 3. TTS providers

- [x] 3.1 Add `model: String` field to `TtsResult` in `tts/TtsProvider.kt`
- [x] 3.2 Return `model = "tts-1"` from `OpenAiTtsProvider.generate()`
- [x] 3.3 Return `model = "eleven_flash_v2_5"` from `ElevenLabsTtsProvider.generate()`
- [x] 3.4 Return `model = "eleven_flash_v2_5"` from `ElevenLabsDialogueTtsProvider.generate()`

## 4. Pipeline

- [x] 4.1 Set `ttsModel = ttsResult.model` in `TtsPipeline.generate()` when saving the episode
- [x] 4.2 Set `ttsModel = ttsResult.model` in `TtsPipeline.generateForExistingEpisode()` when updating the episode

## 5. API

- [x] 5.1 Add `ttsModel: String?` to `EpisodeResponse` and map it in `toResponse()` in `EpisodeController.kt`

## 6. Tests

- [x] 6.1 Update existing TTS-related tests to account for the new `model` field in `TtsResult`
- [x] 6.2 Verify all tests pass with `./mvnw test`

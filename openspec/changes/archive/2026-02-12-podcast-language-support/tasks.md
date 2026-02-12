## 1. Database & Entity

- [x] 1.1 Create Flyway migration `V4__add_podcast_language.sql` adding `language TEXT NOT NULL DEFAULT 'en'` column to the `podcasts` table
- [x] 1.2 Add `language: String = "en"` field to the `Podcast` data class

## 2. Supported Languages

- [x] 2.1 Create `SupportedLanguage` enum with all 57 ISO 639-1 codes, display names, and a `toLocale()` method that resolves to a Java `Locale` (falling back to `Locale.ENGLISH` for unsupported codes)
- [x] 2.2 Write unit tests for `SupportedLanguage`: valid code lookup, invalid code rejection, locale resolution, and English fallback

## 3. API Layer

- [x] 3.1 Add `language: String?` field to `CreatePodcastRequest` and `UpdatePodcastRequest` DTOs
- [x] 3.2 Add `language: String` field to `PodcastResponse` DTO and update the `toResponse()` mapping
- [x] 3.3 Update `PodcastController.create()` to pass `language` (default `"en"`) to the podcast entity
- [x] 3.4 Update `PodcastController.update()` to update `language` from the request (keep existing if not provided)
- [x] 3.5 Add validation in create and update endpoints: return HTTP 400 if `language` is not a valid supported language code
- [x] 3.6 Write controller tests for language field: create with valid/invalid language, update language, default language in response

## 4. LLM Processing

- [x] 4.1 Update `BriefingComposer.compose()` to format the date using the locale from `SupportedLanguage.toLocale()` for the podcast's language instead of `Locale.ENGLISH`
- [x] 4.2 Update `BriefingComposer.compose()` to add a language instruction to the prompt when the podcast's language is not `"en"` (e.g., "Write the entire script in Dutch")
- [x] 4.3 Write unit tests for `BriefingComposer`: verify prompt contains language instruction for non-English podcasts, verify date formatting uses correct locale, verify no language instruction for English podcasts

## 5. TTS Generation

- [x] 5.1 Update `TtsService.generateAudio()` log message to include the podcast's language (e.g., "voice: nova, speed: 1.0, language: nl")

## 6. RSS Feed

- [x] 6.1 Update `FeedGenerator.generate()` to set `feed.language` on the `SyndFeedImpl` using the podcast's language code
- [x] 6.2 Write a unit test for `FeedGenerator`: verify the RSS XML contains the `<language>` element matching the podcast's language

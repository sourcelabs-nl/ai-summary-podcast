## Why

The application currently produces all podcast episodes in English. Users who want briefings in other languages must rely on `customInstructions` as a workaround (e.g., "use Dutch language"), with no validation and no guarantee that the TTS output will sound correct. Adding a first-class `language` field — constrained to languages the OpenAI TTS model actually supports — lets users reliably generate podcasts in their preferred language across both the LLM script and the TTS audio.

## What Changes

- Add a `language` field to the podcast entity, defaulting to `en` (English).
- Restrict allowed values to the set of languages supported by the OpenAI TTS model (based on Whisper language support): Afrikaans, Arabic, Armenian, Azerbaijani, Belarusian, Bosnian, Bulgarian, Catalan, Chinese, Croatian, Czech, Danish, Dutch, English, Estonian, Finnish, French, Galician, German, Greek, Hebrew, Hindi, Hungarian, Icelandic, Indonesian, Italian, Japanese, Kannada, Kazakh, Korean, Latvian, Lithuanian, Macedonian, Malay, Marathi, Maori, Nepali, Norwegian, Persian, Polish, Portuguese, Romanian, Russian, Serbian, Slovak, Slovenian, Spanish, Swahili, Swedish, Tagalog, Tamil, Thai, Turkish, Ukrainian, Urdu, Vietnamese, Welsh.
- Instruct the LLM briefing composer to write the script in the configured language.
- Format the briefing date according to the podcast's language locale.
- Set the RSS feed `<language>` element to match the podcast language.
- Expose the `language` field in podcast CRUD API endpoints.

## Capabilities

### New Capabilities

- `podcast-language`: Per-podcast language selection, restricted to TTS-supported languages, with effects on LLM script composition, date formatting, and RSS feed metadata.

### Modified Capabilities

- `podcast-customization`: Add `language` field alongside existing customization fields (ttsVoice, ttsSpeed, style, etc.) in the podcast entity, CRUD endpoints, and defaults.
- `tts-generation`: TTS audio generation must be aware of the podcast language for logging/observability (OpenAI TTS auto-detects language from input text, so no API parameter change needed).
- `podcast-feed`: RSS feed generation must set the `<language>` element based on the podcast's configured language.
- `llm-processing`: Briefing composer must instruct the LLM to write the script in the podcast's configured language. Date formatting in the prompt must use the matching locale.

## Impact

- **Database**: New migration to add `language` column to the `podcasts` table.
- **Podcast entity**: New `language` field with default `"en"`.
- **API DTOs**: `CreatePodcastRequest`, `UpdatePodcastRequest`, `PodcastResponse` updated with `language` field.
- **Validation**: New enum or constant set of allowed language codes, validated on create/update.
- **BriefingComposer**: Prompt updated to include language instruction; date formatted with matching `Locale`.
- **FeedGenerator**: Sets `feed.language` from podcast language.
- **TtsService**: No API change needed (OpenAI TTS infers language from text), but logging updated to include language.
- **No breaking changes** — the field defaults to `en`, preserving existing behavior.
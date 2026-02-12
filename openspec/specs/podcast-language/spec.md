# Capability: Podcast Language

## Purpose

Per-podcast language selection, restricted to TTS-supported languages, with effects on LLM script composition, date formatting, and RSS feed metadata.

## Requirements

### Requirement: Supported languages enumeration
The system SHALL define a set of supported languages, each represented by its ISO 639-1 two-letter code and a display name. The supported set SHALL match the languages supported by the OpenAI TTS model (based on Whisper): Afrikaans (af), Arabic (ar), Armenian (hy), Azerbaijani (az), Belarusian (be), Bosnian (bs), Bulgarian (bg), Catalan (ca), Chinese (zh), Croatian (hr), Czech (cs), Danish (da), Dutch (nl), English (en), Estonian (et), Finnish (fi), French (fr), Galician (gl), German (de), Greek (el), Hebrew (he), Hindi (hi), Hungarian (hu), Icelandic (is), Indonesian (id), Italian (it), Japanese (ja), Kannada (kn), Kazakh (kk), Korean (ko), Latvian (lv), Lithuanian (lt), Macedonian (mk), Malay (ms), Marathi (mr), Maori (mi), Nepali (ne), Norwegian (no), Persian (fa), Polish (pl), Portuguese (pt), Romanian (ro), Russian (ru), Serbian (sr), Slovak (sk), Slovenian (sl), Spanish (es), Swahili (sw), Swedish (sv), Tagalog (tl), Tamil (ta), Thai (th), Turkish (tr), Ukrainian (uk), Urdu (ur), Vietnamese (vi), Welsh (cy).

#### Scenario: Valid language code accepted
- **WHEN** a podcast is created or updated with `language` set to `"nl"`
- **THEN** the value is accepted and stored

#### Scenario: Invalid language code rejected
- **WHEN** a podcast is created or updated with `language` set to `"xx"`
- **THEN** the system returns HTTP 400 with an error message indicating the language is not supported

#### Scenario: List supported languages
- **WHEN** the system validates a language code
- **THEN** it checks against the defined set of 57 supported ISO 639-1 codes

### Requirement: Language-aware locale resolution
The system SHALL resolve each supported language code to a Java `Locale` for date formatting. If a language code does not have a corresponding Java `Locale`, the system SHALL fall back to `Locale.ENGLISH`.

#### Scenario: Language with available locale
- **WHEN** the language is `"nl"` (Dutch)
- **THEN** the resolved locale is `Locale("nl")` and dates format as e.g. "donderdag 12 februari 2026"

#### Scenario: Language without available locale
- **WHEN** the language is `"mi"` (Maori) and no Java Locale exists for it
- **THEN** the resolved locale falls back to `Locale.ENGLISH`

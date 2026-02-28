## Why

Recurring proper nouns in tech/AI news (e.g., "Anthropic", "Mistral", "LLaMA", "Hugging Face") are frequently mispronounced by TTS engines. Additionally, non-English names like Dutch names "Jarno" (should be "/jɑrnoː/" — "Yarno") and "Stephan" (should be "/ˈsteːfɑn/" — "Stefan") get anglicized incorrectly. Inworld TTS supports inline IPA phoneme notation (`/phoneme/`), but the LLM has no way to know the correct pronunciation for domain-specific terms or foreign names. A per-podcast configurable pronunciation dictionary gives users control over how names and terms are pronounced in their podcast episodes.

## What Changes

- Add a `pronunciations` field to the Podcast entity — a `Map<String, String>` mapping terms to their IPA phoneme notation (e.g., `{"Anthropic": "/ænˈθɹɒpɪk/", "LLaMA": "/ˈlɑːmə/", "Jarno": "/jɑrnoː/", "Stephan": "/ˈsteːfɑn/", "Sourcelabs": "/sɔːrsˌlæbz/"}`)
- Inject the pronunciation dictionary into the Inworld script guidelines so the LLM includes `/phoneme/` notation on first occurrence of each term
- Accept `pronunciations` in podcast create/update endpoints and include in GET responses
- Add a database migration for the new column

## Capabilities

### New Capabilities

- `pronunciation-dictionary`: Per-podcast pronunciation dictionary that maps terms to IPA phonemes, injected into TTS script guidelines for correct pronunciation of proper nouns

### Modified Capabilities

- `podcast-customization`: Add `pronunciations` field to podcast CRUD endpoints
- `inworld-tts`: Inject pronunciation dictionary entries into Inworld script guidelines when available

## Impact

- `Podcast.kt` — new `pronunciations` field
- `PodcastController.kt` — accept/return `pronunciations` in create/update/get
- `PodcastService.kt` — propagate `pronunciations` field
- `InworldTtsProvider.kt` — include pronunciation entries in `scriptGuidelines()` output
- `TtsProvider.kt` — extend `scriptGuidelines()` signature to accept pronunciation map (or pass via a context object)
- New Flyway migration for the `pronunciations` column
- Tests for all modified components

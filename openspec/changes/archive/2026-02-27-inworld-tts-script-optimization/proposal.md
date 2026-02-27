## Why

The Inworld TTS documentation specifies best practices for text formatting, normalization, and markup that the current script generation pipeline does not fully follow. This leads to suboptimal audio quality — numbers read as digits instead of words, markdown artifacts read aloud, and `**double asterisks**` pronounced as literal characters. Aligning the script guidelines and API parameters with Inworld's recommendations will produce more natural-sounding podcast episodes.

## What Changes

- Expand `InworldTtsProvider.scriptGuidelines()` with text normalization rules (numbers, dates, currencies in spoken form), anti-markdown rules, contractions guidance, and a warning against `**double asterisks**`
- Add Inworld-specific post-processing to sanitize LLM output: convert `**double**` to `*single*` asterisks, strip markdown artifacts, strip emojis, whitelist supported non-verbal tags (strip unsupported `[emotion]` tags the LLM invents)
- Pass `applyTextNormalization: true` to the Inworld API as a safety net for any numbers/dates the LLM misses
- Set a default temperature of `0.8` for non-realtime podcast generation when no explicit temperature is configured

## Capabilities

### New Capabilities

- `inworld-script-postprocessing`: Post-processing pipeline that sanitizes LLM-generated scripts for Inworld TTS — converts double asterisks, strips markdown, removes emojis, and whitelists supported non-verbal tags

### Modified Capabilities

- `inworld-tts`: Add `applyTextNormalization` parameter to API requests and default temperature of `0.8`
- `tts-script-profile`: Expand Inworld script guidelines with text normalization, anti-markdown, contractions, and punctuation rules

## Impact

- `InworldTtsProvider.kt` — expanded script guidelines, post-processing integration, default temperature
- `InworldApiClient.kt` — add `applyTextNormalization` to request body
- New post-processing utility class for Inworld script sanitization
- Tests for post-processing logic and updated guideline assertions

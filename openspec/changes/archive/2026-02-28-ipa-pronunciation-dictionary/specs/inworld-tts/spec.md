## MODIFIED Requirements

### Requirement: Inworld TTS script guidelines
The `InworldTtsProvider` SHALL return style-aware script guidelines via `scriptGuidelines(style, pronunciations)`. The guidelines SHALL instruct the LLM to use Inworld-specific markup:
- Non-verbal tags: `[sigh]`, `[laugh]`, `[breathe]`, `[cough]`, `[clear_throat]`, `[yawn]`
- Emphasis: `*word*` (single asterisks) for stressed words
- Pacing: ellipsis (`...`) for trailing pauses, exclamation marks for excitement
- IPA phonemes: `/phoneme/` for precise pronunciation of proper nouns

The guidelines SHALL additionally include:
- Text normalization: write all numbers, dates, currencies, and symbols in fully spoken form
- Anti-markdown: never use markdown formatting; never use `**double asterisks**` as the TTS engine reads asterisk characters aloud
- Contractions: use natural contractions throughout for spoken naturalness
- Punctuation: always end sentences with proper punctuation for correct pacing

For `CASUAL` and `DIALOGUE` styles, guidelines SHALL additionally encourage natural filler words (`uh`, `um`, `well`, `you know`). For `EXECUTIVE_SUMMARY` and `NEWS_BRIEFING` styles, guidelines SHALL instruct to avoid filler words and minimize non-verbal tags.

When `pronunciations` is non-empty, the guidelines SHALL append a "Pronunciation Guide" section listing each term and its IPA phoneme, with an instruction to use the IPA notation on the first occurrence of each term in the script. When `pronunciations` is empty, no pronunciation section SHALL be appended.

#### Scenario: Casual style guidelines include filler words
- **WHEN** `scriptGuidelines(PodcastStyle.CASUAL, emptyMap())` is called
- **THEN** the returned text includes instructions to use filler words naturally

#### Scenario: Executive summary guidelines suppress filler words
- **WHEN** `scriptGuidelines(PodcastStyle.EXECUTIVE_SUMMARY, emptyMap())` is called
- **THEN** the returned text instructs to avoid filler words and minimize non-verbal tags

#### Scenario: All styles include core markup and formatting instructions
- **WHEN** `scriptGuidelines()` is called for any style
- **THEN** the returned text includes instructions for non-verbal tags, emphasis, pacing, IPA phonemes, text normalization, anti-markdown, contractions, and punctuation

#### Scenario: Guidelines warn against double asterisks
- **WHEN** `scriptGuidelines()` is called for any style
- **THEN** the returned text explicitly warns that `**double asterisks**` will cause the TTS engine to read asterisk characters aloud

#### Scenario: Guidelines include pronunciation dictionary
- **WHEN** `scriptGuidelines(PodcastStyle.CASUAL, mapOf("Anthropic" to "/ænˈθɹɒpɪk/", "Jarno" to "/jɑrnoː/"))` is called
- **THEN** the returned text includes a "Pronunciation Guide" section with both entries and instructs the LLM to use IPA notation on first occurrence

#### Scenario: Guidelines omit pronunciation section when empty
- **WHEN** `scriptGuidelines(PodcastStyle.CASUAL, emptyMap())` is called
- **THEN** the returned text does not include a "Pronunciation Guide" section

## Context

The application currently generates all podcast briefings in English. The `BriefingComposer` hardcodes `Locale.ENGLISH` for date formatting and does not instruct the LLM to write in any particular language. The OpenAI TTS API auto-detects language from input text (no explicit language parameter), following Whisper's supported language set of ~57 languages. Users can work around this with `customInstructions`, but there is no validation or structured support.

## Goals / Non-Goals

**Goals:**
- Add a `language` field to the podcast entity, exposed in all CRUD endpoints
- Restrict allowed values to languages supported by the OpenAI TTS model
- Instruct the LLM to compose the briefing script in the selected language
- Format dates in the prompt using the language's locale
- Set the RSS `<language>` element on the feed

**Non-Goals:**
- Translating source articles before summarization (articles are summarized as-is; the LLM composes the final script in the target language)
- Changing the relevance filtering or summarization language (these operate on source content)
- Supporting dialect/accent selection within a language (not supported by OpenAI TTS)
- Adding a language parameter to the TTS API call (OpenAI TTS auto-detects from text)

## Decisions

### Decision 1: Language representation as ISO 639-1 codes

Store language as a two-letter ISO 639-1 code (e.g., `en`, `nl`, `fr`, `de`). This is compact, well-understood, and maps cleanly to Java `Locale` for date formatting and to RSS `<language>` element values.

**Alternative considered:** Full language names (e.g., "English", "Dutch") — rejected because they are harder to validate, locale-sensitive themselves, and don't map to standard APIs.

**Alternative considered:** BCP 47 tags (e.g., "en-US", "nl-NL") — rejected as unnecessarily granular since OpenAI TTS doesn't support dialect/accent selection.

### Decision 2: Allowed languages defined as an enum-like constant set

Define a `SupportedLanguage` enum in the domain layer mapping each ISO 639-1 code to its display name and Java `Locale`. Validation on create/update rejects codes not in this set. This ensures users can only select languages that produce acceptable TTS output.

The set is based on the OpenAI TTS supported languages (which follow Whisper): af, ar, hy, az, be, bs, bg, ca, zh, hr, cs, da, nl, en, et, fi, fr, gl, de, el, he, hi, hu, is, id, it, ja, kn, kk, ko, lv, lt, mk, ms, mr, mi, ne, no, fa, pl, pt, ro, ru, sr, sk, sl, es, sw, sv, tl, ta, th, tr, uk, ur, vi, cy.

### Decision 3: Language affects only the briefing composition step

The language field instructs the `BriefingComposer` to write the script in the target language. Relevance filtering and article summarization remain language-agnostic (they operate on source article content). This keeps the pipeline simple — the LLM handles the "translation" naturally during composition.

### Decision 4: Date formatting uses the podcast's locale

The `BriefingComposer` currently formats the date with `Locale.ENGLISH`. With this change, it will use the `Locale` corresponding to the podcast's language code. For example, a Dutch podcast would format dates like "donderdag 12 februari 2026".

### Decision 5: No TTS API parameter change

OpenAI's TTS API auto-detects language from input text. There is no explicit language parameter in the API or in Spring AI's `OpenAiAudioSpeechOptions`. Since the briefing script will already be in the target language, TTS will naturally produce audio in that language. The `TtsService` only needs updated logging to include the language for observability.

### Decision 6: RSS feed language element

The `FeedGenerator` will set `feed.language` on the `SyndFeedImpl` to the podcast's language code. This is a standard RSS 2.0 element that helps podcast apps categorize and display feeds correctly.

### Decision 7: Database migration adds column with default

A new Flyway migration adds a `language` column to the `podcasts` table with `DEFAULT 'en'` and `NOT NULL`. Existing podcasts automatically get English as their language, preserving backward compatibility.

## Risks / Trade-offs

- **TTS quality varies by language** — OpenAI TTS is optimized for English; other languages may have lower quality pronunciation or unnatural prosody. → Mitigation: This is inherent to the TTS provider. Users choose their language knowing this trade-off. The restriction to supported languages prevents completely unsupported inputs.

- **LLM composition quality in non-English languages** — The LLM may produce lower quality scripts in some languages. → Mitigation: Users can choose their LLM model per podcast; more capable models produce better multilingual output.

- **Locale mapping gaps** — Some ISO 639-1 codes (e.g., `mi` for Maori) may not have full Java `Locale` support for date formatting. → Mitigation: Fall back to English locale for date formatting if the language's locale is not available in the JVM.
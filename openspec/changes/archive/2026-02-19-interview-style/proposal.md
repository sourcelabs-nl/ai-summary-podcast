## Why

The current `dialogue` style produces a symmetrical conversation where both speakers contribute equally to delivering news content. This can sound unnatural — real podcast interviews have an asymmetric dynamic where one person asks questions and another delivers expertise. An interviewer/expert style, where the interviewer acts as an audience surrogate asking questions and the expert delivers the briefing content, produces more natural-sounding podcast audio.

## What Changes

- **New `interview` podcast style**: A new composition style with fixed `interviewer` and `expert` roles. The interviewer asks questions, bridges topics, and reacts as an audience surrogate (~20% of words). The expert delivers news, context, and analysis (~80% of words).
- **New `InterviewComposer` component**: Dedicated composer with an asymmetric prompt that produces interviewer/expert dialogue using XML speaker tags.
- **New `speakerNames` field on Podcast**: A `Map<String, String>` mapping role keys to display names (e.g., `{"interviewer": "Alice", "expert": "Bob"}`). Names appear in spoken text while tags remain role-based for TTS mapping.
- **`speakerNames` support in `DialogueComposer`**: The existing dialogue style also benefits from speaker names when provided.

## Capabilities

### New Capabilities
- `interview-composition`: Interview-style dialogue composition with fixed interviewer/expert roles, asymmetric prompt, and speaker name support.

### Modified Capabilities
- `dialogue-composition`: DialogueComposer gains support for `speakerNames` to give speakers display names in conversation.
- `podcast-customization`: New `interview` style option and new `speakerNames` field on podcast CRUD endpoints.

## Impact

- **Podcast entity**: New `speaker_names` column (TEXT, nullable, JSON map) + Flyway migration.
- **LLM pipeline**: New routing branch for `style == "interview"` → `InterviewComposer`.
- **Podcast API**: `speakerNames` accepted in create/update, returned in GET. Validation: `interview` style requires ElevenLabs + exactly `interviewer`/`expert` voice roles.
- **TTS pipeline**: No changes — already supports role-based voice mapping via `DialogueScriptParser`.

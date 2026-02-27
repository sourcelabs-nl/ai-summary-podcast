## ADDED Requirements

### Requirement: Inworld script post-processor sanitizes LLM output
The system SHALL provide an `InworldScriptPostProcessor` object with a `process(script: String): String` method that sanitizes LLM-generated scripts for Inworld TTS consumption. The post-processor SHALL apply transformations in the following order:
1. Convert double-asterisk emphasis (`**word**`) to single-asterisk emphasis (`*word*`)
2. Strip markdown headers (lines starting with `#`)
3. Strip markdown bullet points (lines starting with `- ` or `* ` — only when followed by a space to preserve `*emphasis*`)
4. Convert markdown links `[text](url)` to just `text`
5. Strip emoji characters
6. Whitelist non-verbal tags — keep only `[sigh]`, `[laugh]`, `[breathe]`, `[cough]`, `[clear_throat]`, `[yawn]` and strip any other `[word]` tags

#### Scenario: Double asterisks converted to single
- **WHEN** the script contains `**important**`
- **THEN** the post-processor converts it to `*important*`

#### Scenario: Single asterisks preserved
- **WHEN** the script contains `*emphasis*`
- **THEN** the post-processor leaves it unchanged

#### Scenario: Markdown headers stripped
- **WHEN** the script contains a line `## Breaking News`
- **THEN** the post-processor removes the entire line

#### Scenario: Markdown bullets stripped
- **WHEN** the script contains a line `- First item`
- **THEN** the post-processor removes the bullet prefix, keeping only `First item`

#### Scenario: Emphasis asterisks at line start not stripped as bullets
- **WHEN** the script contains `*stressed word* in a sentence`
- **THEN** the post-processor preserves the line unchanged (does not treat `*` as a bullet)

#### Scenario: Markdown links converted to plain text
- **WHEN** the script contains `[Anthropic](https://anthropic.com)`
- **THEN** the post-processor converts it to `Anthropic`

#### Scenario: Emojis stripped
- **WHEN** the script contains `Great news! 🎉 The update is here`
- **THEN** the post-processor removes the emoji, producing `Great news!  The update is here`

#### Scenario: Supported non-verbal tags preserved
- **WHEN** the script contains `[sigh] I can't believe it`
- **THEN** the post-processor preserves the `[sigh]` tag

#### Scenario: Unsupported tags stripped
- **WHEN** the script contains `[cheerfully] Welcome to the show`
- **THEN** the post-processor removes the `[cheerfully]` tag, producing `Welcome to the show`

#### Scenario: Multiple transformations applied together
- **WHEN** the script contains `## Intro\n**Welcome** to the show! 🎉 [excitedly] Let's begin.`
- **THEN** the post-processor produces `*Welcome* to the show!  Let's begin.`

### Requirement: Inworld provider applies post-processing before TTS generation
The `InworldTtsProvider` SHALL apply `InworldScriptPostProcessor.process()` to the script text before passing it to `TextChunker` or the Inworld API. For monologue styles, the full script SHALL be post-processed. For dialogue styles, each `DialogueTurn.text` SHALL be post-processed individually.

#### Scenario: Monologue script post-processed before chunking
- **WHEN** a monologue script with markdown artifacts is sent to `InworldTtsProvider`
- **THEN** the provider post-processes the script before chunking and sending to the API

#### Scenario: Dialogue turns post-processed individually
- **WHEN** a dialogue script with unsupported `[emotion]` tags is sent to `InworldTtsProvider`
- **THEN** each turn's text is post-processed before chunking and sending to the API

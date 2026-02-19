## MODIFIED Requirements

### Requirement: ElevenLabs Text-to-Dialogue provider
The system SHALL implement `TtsProvider` as `ElevenLabsDialogueTtsProvider` that generates multi-speaker audio via the ElevenLabs `POST /v1/text-to-dialogue` endpoint. The provider SHALL parse the script's XML-style speaker tags into an array of `{text, voice_id}` inputs. When the total text length of all inputs exceeds 5000 characters, the provider SHALL split the inputs into batches where each batch's total text length stays under 5000 characters. Turns SHALL NOT be split across batches. Each batch SHALL be sent as a separate API call. When multiple batches are produced, the result SHALL have `requiresConcatenation = true` and contain one audio chunk per batch. When only one batch is needed, the result SHALL have `requiresConcatenation = false` with a single audio chunk.

#### Scenario: Short dialogue fits in single batch
- **WHEN** a dialogue script has total text length under 5000 characters
- **THEN** the provider makes a single API call and returns `requiresConcatenation = false`

#### Scenario: Long dialogue split into multiple batches
- **WHEN** a dialogue script has total text length of 9000 characters
- **THEN** the provider splits turns into batches under 5000 characters each, makes one API call per batch, and returns `requiresConcatenation = true` with one audio chunk per batch

#### Scenario: Batch boundary falls between turns
- **WHEN** turns are grouped into batches
- **THEN** each turn is entirely within one batch — no turn is split across batches

#### Scenario: Single turn under limit starts new batch
- **WHEN** adding a turn to the current batch would exceed 5000 characters
- **THEN** a new batch is started with that turn

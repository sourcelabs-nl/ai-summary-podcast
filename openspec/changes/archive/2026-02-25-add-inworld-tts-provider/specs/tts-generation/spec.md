## MODIFIED Requirements

### Requirement: Text chunking at sentence boundaries
The system SHALL split the briefing script into chunks that respect a configurable maximum chunk size. The `TextChunker.chunk()` method SHALL accept a `maxChunkSize: Int` parameter (defaulting to 4096 for backward compatibility). Chunks SHALL be split at sentence boundaries (period, exclamation mark, question mark followed by whitespace) to avoid mid-sentence audio cuts.

#### Scenario: Long script split into multiple chunks
- **WHEN** a briefing script of 8000 characters is processed with `maxChunkSize = 4096`
- **THEN** the script is split into 2 or more chunks, each at most 4096 characters, with splits occurring at sentence boundaries

#### Scenario: Short script kept as single chunk
- **WHEN** a briefing script of 1500 characters is processed with `maxChunkSize = 2000`
- **THEN** the script is sent as a single chunk without splitting

#### Scenario: Very long sentence handling
- **WHEN** a single sentence exceeds the configured `maxChunkSize`
- **THEN** the sentence is split at the nearest whitespace before the limit

#### Scenario: Inworld chunk size used
- **WHEN** a script is chunked with `maxChunkSize = 2000`
- **THEN** each chunk is at most 2000 characters

#### Scenario: Default chunk size for backward compatibility
- **WHEN** `TextChunker.chunk(text)` is called without a `maxChunkSize` parameter
- **THEN** the default max chunk size of 4096 is used

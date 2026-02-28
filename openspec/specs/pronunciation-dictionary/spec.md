### Requirement: Per-podcast pronunciation dictionary
Each podcast SHALL have an optional `pronunciations` field (TEXT, nullable, stored as JSON map). The map keys SHALL be terms (proper nouns, brand names, or foreign names) and values SHALL be IPA phoneme notation strings (e.g., `{"Anthropic": "/ænˈθɹɒpɪk/", "Jarno": "/jɑrnoː/", "Stephan": "/ˈsteːfɑn/", "Sourcelabs": "/sɔːrsˌlæbz/"}`). When the pronunciation dictionary is non-empty and the TTS provider supports IPA phonemes, the dictionary SHALL be injected into the LLM script guidelines as a "Pronunciation Guide" section. The LLM SHALL be instructed to use the IPA notation on the first occurrence of each term in the script. When the dictionary is null or empty, no pronunciation section SHALL be added to the guidelines.

#### Scenario: Podcast with pronunciation dictionary
- **WHEN** a podcast has `pronunciations` set to `{"Anthropic": "/ænˈθɹɒpɪk/", "LLaMA": "/ˈlɑːmə/"}`
- **AND** the TTS provider is Inworld
- **THEN** the script guidelines include a "Pronunciation Guide" section listing both terms with their IPA phonemes

#### Scenario: Podcast with empty pronunciation dictionary
- **WHEN** a podcast has `pronunciations` set to `{}`
- **THEN** no pronunciation section is added to the script guidelines

#### Scenario: Podcast with null pronunciation dictionary
- **WHEN** a podcast has `pronunciations` set to null
- **THEN** no pronunciation section is added to the script guidelines

#### Scenario: Dutch name pronunciation
- **WHEN** a podcast has `pronunciations` set to `{"Jarno": "/jɑrnoː/", "Stephan": "/ˈsteːfɑn/"}`
- **THEN** the script guidelines instruct the LLM to use `/jɑrnoː/` on first mention of "Jarno" and `/ˈsteːfɑn/` on first mention of "Stephan"

#### Scenario: First occurrence only instruction
- **WHEN** pronunciations are injected into guidelines
- **THEN** the guidelines instruct the LLM to apply IPA notation only on the first occurrence of each term in the script

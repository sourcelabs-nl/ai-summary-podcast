## MODIFIED Requirements

### Requirement: Settings sub-tabs
The settings form SHALL be organized into four sub-tabs: General, LLM, TTS, and Content. The General tab SHALL be the default active tab.

#### Scenario: General tab fields
- **WHEN** the General tab is active
- **THEN** the form displays editable fields for: name (text), topic (text), language (text), style (select), cron (text), customInstructions (textarea), requireReview (checkbox)

#### Scenario: LLM tab fields
- **WHEN** the LLM tab is active
- **THEN** the form displays editable fields for: llmModels (key-value editor), relevanceThreshold (number), maxLlmCostCents (number), fullBodyThreshold (number), maxArticleAgeDays (number), targetWords (number)

#### Scenario: TTS tab fields
- **WHEN** the TTS tab is active
- **THEN** the form displays editable fields for: ttsProvider (select), ttsVoices (key-value editor), ttsSettings (key-value editor), speakerNames (key-value editor)

#### Scenario: Content tab fields
- **WHEN** the Content tab is active
- **THEN** the form displays editable fields for: sponsor (key-value editor), pronunciations (key-value editor)

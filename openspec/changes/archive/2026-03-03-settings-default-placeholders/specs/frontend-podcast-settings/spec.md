## ADDED Requirements

### Requirement: System default placeholders on nullable fields
Nullable number fields in the settings form SHALL display placeholder text showing the system default value when the field is empty. The placeholder values SHALL be fetched from the `GET /config/defaults` API endpoint. The placeholder format SHALL be `{value} (system default)`.

#### Scenario: Empty target words shows default
- **WHEN** the LLM tab is active and targetWords is null/empty
- **THEN** the input displays placeholder text with the targetWords value from the defaults endpoint followed by "(system default)"

#### Scenario: Empty max LLM cost shows default
- **WHEN** the LLM tab is active and maxLlmCostCents is null/empty
- **THEN** the input displays placeholder text with the maxLlmCostCents value from the defaults endpoint followed by "(system default)"

#### Scenario: Empty full body threshold shows default
- **WHEN** the LLM tab is active and fullBodyThreshold is null/empty
- **THEN** the input displays placeholder text with the fullBodyThreshold value from the defaults endpoint followed by "(system default)"

#### Scenario: Empty max article age shows default
- **WHEN** the LLM tab is active and maxArticleAgeDays is null/empty
- **THEN** the input displays placeholder text with the maxArticleAgeDays value from the defaults endpoint followed by "(system default)"

#### Scenario: Explicit value hides placeholder
- **WHEN** a nullable field has an explicit user-set value
- **THEN** the placeholder is not visible and the user's value is displayed

### Requirement: LLM models default hint
The LLM Models key-value editor SHALL display a helper text below it showing the system default model assignments fetched from the `GET /config/defaults` endpoint.

#### Scenario: Default models displayed
- **WHEN** the LLM tab is active and defaults have been loaded
- **THEN** a helper text is displayed below the editor listing default model assignments (e.g., "System defaults: filter = openai/gpt-4o-mini, compose = anthropic/claude-sonnet-4.6")

#### Scenario: Defaults not loaded
- **WHEN** the defaults endpoint fails or has not loaded yet
- **THEN** no helper text or placeholder is displayed

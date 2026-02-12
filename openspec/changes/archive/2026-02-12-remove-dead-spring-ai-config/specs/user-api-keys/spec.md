## MODIFIED Requirements

### Requirement: API key resolution with fallback
When the pipeline needs an API key for a provider, the system SHALL resolve it in this order: (1) decrypt the owning user's key for that provider, (2) fall back to the corresponding environment variable (`OPENROUTER_API_KEY` for provider `"openrouter"`, `OPENAI_API_KEY` for provider `"openai"`). If neither exists, the pipeline step SHALL fail with a clear error. No Spring AI auto-configured beans SHALL be involved in key resolution.

#### Scenario: User has a key for the provider
- **WHEN** the pipeline runs for a podcast whose owning user has an encrypted "openrouter" API key stored
- **THEN** the system decrypts the key and uses it for LLM calls

#### Scenario: User has no key, global env var exists
- **WHEN** the pipeline runs for a podcast whose owning user has no "openrouter" API key, but the `OPENROUTER_API_KEY` environment variable is set
- **THEN** the system uses the environment variable value for LLM calls

#### Scenario: No key available anywhere
- **WHEN** the pipeline runs for a podcast whose owning user has no "openrouter" API key and the `OPENROUTER_API_KEY` environment variable is not set
- **THEN** the pipeline step fails with an error indicating no API key is available for the provider

#### Scenario: Spring AI auto-configuration excluded
- **WHEN** the application starts
- **THEN** no Spring AI auto-configured chat or audio beans are created, and no `spring.ai` properties are required

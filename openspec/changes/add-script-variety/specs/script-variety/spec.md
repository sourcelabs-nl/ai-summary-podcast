## ADDED Requirements

### Requirement: Per-episode prompt variety rotation

The compose stage SHALL deterministically rotate a set of prompt-shape dimensions per episode so that consecutive episodes from the same podcast do not produce byte-identical scaffolding (openings, transitions, sign-offs). Selection MUST be a pure function of `(podcastId, episodeDate)` so that regenerating the same episode yields the same selection.

The rotated dimensions MUST include at minimum:

- Opening style (e.g. cold-open question, shocking-stat, scene-set, contrarian take, first-person hook).
- Transition vocabulary set.
- Sign-off shape (e.g. recap-first, forward-look, call-to-action, quote-of-the-day).

#### Scenario: Same episode regenerated gets the same selection

- **WHEN** the compose stage runs twice for the same `(podcastId, episodeDate)`
- **THEN** the variety picker returns identical opening-style, transition-vocabulary, and sign-off-shape selections both times

#### Scenario: Consecutive days produce different selections

- **WHEN** the compose stage runs for the same podcast on five consecutive dates
- **THEN** at least three of the five episodes have a distinct opening-style selection

### Requirement: Compose-stage temperature is configurable

The compose stage SHALL pass a `temperature` value to the LLM chat options. The temperature SHALL default to `0.95` and SHALL be overridable per podcast via the `composeTemperature` field. Temperature MUST be clamped to `[0.0, 2.0]` server-side.

#### Scenario: Default temperature is applied

- **WHEN** a podcast has no `composeTemperature` override
- **THEN** the compose stage calls the LLM with `temperature=0.95`

#### Scenario: Per-podcast temperature override is applied

- **WHEN** a podcast has `composeTemperature=0.6`
- **THEN** the compose stage calls the LLM with `temperature=0.6`

#### Scenario: Out-of-range temperature is rejected

- **WHEN** the API receives `composeTemperature=3.5` on a podcast update
- **THEN** the value is rejected with HTTP 422 with a message identifying the allowed range

### Requirement: Composer prompts contain no verbatim example phrases

The compose-stage prompts for monologue, dialogue, and interview composers SHALL NOT include verbatim example sentences that have previously appeared in generated scripts (e.g. literal `"But here's where it gets really interesting..."`, literal `"Coming up: AI agents going rogue..."`, literal `"Stephan, thanks as always"`, literal interruption samples like `"Wait, wait — did you say 100x?!"`). Structural beats MAY be described in abstract terms; concrete sample phrases that the LLM is prone to copy verbatim MUST be removed.

#### Scenario: Prompt contains no banned phrases

- **WHEN** the compose prompt is built for any style (`news-briefing`, `casual`, `deep-dive`, `executive-summary`, `dialogue`, `interview`)
- **THEN** the prompt string does not contain any of the documented banned example phrases verbatim

## Context

The episode generation pipeline has two entry points: the scheduled `BriefingGenerationScheduler` and the manual `PodcastController.generate()` endpoint. The scheduler properly saves episode-article links and generates recaps, but the manual endpoint reimplements generation logic without these steps. This causes discarded manually-generated episodes to leave articles permanently stuck as `is_processed = true`, and subsequent episodes receive very few articles — leading the LLM to hallucinate content from its training data.

## Goals / Non-Goals

**Goals:**
- Ensure all episode generation paths save episode-article links and generate recaps
- Ensure discarding any episode correctly resets its articles for reprocessing
- Prevent the LLM from generating content not grounded in the provided articles
- Establish a development guideline preventing controllers from duplicating domain logic

**Non-Goals:**
- Cross-source semantic deduplication (different sources posting about the same news is acceptable)
- Changing the episode status lifecycle or review workflow
- Modifying the LLM pipeline itself (scoring, summarization stages remain unchanged)

## Decisions

### Decision 1: Extract shared generation logic into `EpisodeService`

**Choice:** Move the episode creation, article-link saving, and recap generation logic from `BriefingGenerationScheduler` into `EpisodeService`, and have both the scheduler and `PodcastController.generate()` delegate to it.

**Rationale:** The controller currently reimplements the scheduler's post-pipeline logic (save episode, save links, generate recap, update lastGeneratedAt). Extracting to a service ensures a single code path and prevents future divergence.

**Alternative considered:** Just add the missing calls to `PodcastController.generate()` — rejected because it would still leave duplicated logic that could diverge again.

### Decision 2: Add grounding instructions to all three composer prompts

**Choice:** Add explicit instructions to `BriefingComposer`, `DialogueComposer`, and `InterviewComposer` requiring the LLM to only discuss content present in the provided articles, and to produce shorter scripts when fewer articles are available.

**Rationale:** The LLM currently embellishes sparse input with its training knowledge. A clear prompt constraint addresses this at the source. We combine a positive instruction ("only discuss...") with a practical fallback ("produce a shorter script").

### Decision 3: Add CLAUDE.md guideline for controller delegation

**Choice:** Add a principle to the project `CLAUDE.md` stating that controllers must delegate to service/domain logic and must not reimplement pipeline or business logic.

**Rationale:** This bug was caused by the controller reimplementing what the scheduler does. A documented guideline prevents this pattern from recurring.

## Risks / Trade-offs

- **Shorter episodes when few articles are available** → Acceptable trade-off vs. hallucinated content. Users can always regenerate after more articles arrive.
- **Grounding instruction may be imperfect** → LLMs may still occasionally embellish. The instruction significantly reduces but cannot fully eliminate hallucination. Monitoring episode quality remains important.
- **Existing episodes without article links** → Episodes already in the database without links cannot be retroactively fixed. The discard handler should handle this gracefully (log a warning, reset no articles).

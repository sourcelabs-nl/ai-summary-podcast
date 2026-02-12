## Context

The `BriefingComposer.compose()` method builds a prompt from article summaries and sends it to the LLM. Currently the prompt receives only article titles and summaries. The `Podcast` entity (with `name` and `topic`) and `Article` entity (with `sourceId` and `url`) are already available as parameters, but their metadata is not included in the prompt.

## Goals / Non-Goals

**Goals:**
- Include the current date in the prompt so the LLM can mention it in the introduction
- Include podcast name and topic in the prompt so the LLM can introduce the show
- Include article source information (source name or URL) in the summary block so the LLM can subtly attribute information
- Keep source attribution natural — "according to TechCrunch" style, not a formal citation

**Non-Goals:**
- Changing the article data model or storing additional source metadata
- Adding source URLs to the spoken script (URLs are not TTS-friendly)
- Modifying the RSS feed XML

## Decisions

### Decision 1: Pass date as formatted string in the prompt header

**Choice:** Use `LocalDate.now()` formatted as a human-readable date (e.g., "Wednesday, February 12, 2026") and include it in the prompt context block above the article summaries.

**Rationale:** A human-readable format lets the LLM naturally weave the date into the introduction ("Today, Wednesday February 12th..."). ISO format would sound robotic when spoken.

### Decision 2: Include podcast name and topic in the prompt context

**Choice:** Add the podcast's `name` and `topic` fields to the prompt context and add an instruction to mention them in the introduction.

**Rationale:** Both fields are already available on the `Podcast` parameter. This gives the LLM the identity of the show to open with, e.g., "Welcome to AI Weekly, your briefing on artificial intelligence..."

### Decision 3: Add source name to each article in the summary block

**Choice:** Derive a readable source name from the article's `sourceId` and include it alongside each article's title and summary. The `sourceId` is the Source entity ID (a UUID), so we need to resolve it to the source URL/type. Since the `Article` already has a `url` field, we can extract the domain name from it (e.g., `techcrunch.com` from `https://techcrunch.com/2026/...`) as a lightweight approach.

**Rationale:** Extracting the domain from the article URL requires no additional database queries or schema changes. Domain names like "TechCrunch" or "The Verge" are natural to say aloud. The LLM is instructed to weave these in subtly ("as reported by TechCrunch...").

**Alternative considered:** Resolving `sourceId` to the `Source` entity to get its URL/type — rejected because it adds a database query and the article URL already contains the information needed.

## Risks / Trade-offs

- [Risk] LLM may over-attribute sources, making the script sound like a bibliography → Mitigation: Prompt instruction says "subtly" and "sparingly" reference sources
- [Risk] Domain extraction may produce ugly names for some URLs → Mitigation: Domain names are generally readable; edge cases are acceptable for now

## 1. Database & Entity

- [x] 1.1 Create Flyway migration `V12__add_article_author.sql` adding `author TEXT` column to `articles` table
- [x] 1.2 Add `author: String? = null` field to the `Article` data class

## 2. Author Extraction — RSS

- [x] 2.1 Extract author from `SyndEntry.author` (fallback to first `SyndEntry.authors` name) in `RssFeedFetcher` and pass to `Article` constructor
- [x] 2.2 Add tests for RSS author extraction: author present, authors list fallback, no author available

## 3. Author Extraction — Website

- [x] 3.1 Extract author from `<meta name="author">` / `<meta property="article:author">` in `WebsiteFetcher` and pass to `Article` constructor
- [x] 3.2 Add tests for website author extraction: meta name tag, article:author tag, no author meta tags

## 4. Summarization Prompt

- [x] 4.1 Update the summarization prompt in `ArticleSummarizer` to instruct the LLM to preserve attribution to people, organizations, and studies
- [x] 4.2 Update `ArticleSummarizer` tests to verify the prompt includes the attribution instruction

## 5. Composition Prompt

- [x] 5.1 Update `BriefingComposer.buildPrompt` to include author in the summary block format: `[domain, by Author]` when author is non-null, `[domain]` when null
- [x] 5.2 Update the composition prompt attribution instruction to mention crediting original authors when known
- [x] 5.3 Update `BriefingComposer` tests to verify summary block format with and without author, and updated prompt wording

## 1. Prompt Improvements

- [x] 1.1 Add content-type detection to `ArticleScoreSummarizer` — detect aggregated articles by title prefix "Posts from" and build prompt context accordingly (social media posts vs neutral content framing)
- [x] 1.2 Include `article.author` in prompt context when available — add author name before the content block
- [x] 1.3 Add direct summarization instruction with negative example — instruct LLM to state facts directly, include "say X not Y" example
- [x] 1.4 Replace "Article title" / "Article text" labels with "Content title" / "Content" for non-aggregated articles

## 2. Tests

- [x] 2.1 Update `ArticleScoreSummarizerTest` — verify aggregated article prompt includes social media post context and author name
- [x] 2.2 Add test for non-aggregated article — verify prompt uses neutral "Content" framing
- [x] 2.3 Add test for article without author — verify prompt works correctly when `article.author` is null
- [x] 2.4 Add test for direct summarization instruction — verify prompt includes the negative example instruction

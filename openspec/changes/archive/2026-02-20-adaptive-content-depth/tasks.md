## 1. Configuration

- [x] 1.1 Add `fullBodyThreshold` (default: 5) to `AppProperties.BriefingProperties`
- [x] 1.2 Add optional `fullBodyThreshold: Int?` column to `Podcast` entity and database migration

## 2. Scaled Summary Length

- [x] 2.1 Update `ArticleScoreSummarizer` prompt to scale summary length based on article body word count (<500 words: 2-3 sentences, 500-1500: 4-6 sentences, 1500+: full paragraph)

## 3. Full Body Threshold in Composers

- [x] 3.1 Update `BriefingComposer.buildPrompt` to use full `article.body` when article count is below `fullBodyThreshold`, otherwise use `article.summary ?: article.body`
- [x] 3.2 Apply the same full-body threshold logic to `DialogueComposer`
- [x] 3.3 Apply the same full-body threshold logic to `InterviewComposer`

## 4. Tests

- [x] 4.1 Add tests for `ArticleScoreSummarizer` prompt containing scaled summary instructions based on article body length
- [x] 4.2 Add tests for `BriefingComposer` selecting full body vs summary based on article count threshold
- [x] 4.3 Add tests for `DialogueComposer` full-body threshold behavior
- [x] 4.4 Add tests for `InterviewComposer` full-body threshold behavior

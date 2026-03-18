## 1. Prompt Changes

- [x] 1.1 Update word distribution from 80/20 to 65/35 in `InterviewComposer.buildPrompt()`
- [x] 1.2 Add conditional "coming up" teaser instruction (when `articles.size >= 5`), placed after sponsor message or after introduction if no sponsor
- [x] 1.3 Add strategic cliffhangers engagement technique (2-3 forward hooks per episode)
- [x] 1.4 Replace "natural interruptions" with "spontaneous interruptions" — 5 typed examples (excited, skeptical, confused, connecting dots, disagreement) + expert pushback
- [x] 1.5 Add strict turn-length enforcement rule (expert max 3-4 sentences, framed as hard rule with listener drop-off rationale)

## 2. Tests

- [x] 2.1 Update existing word distribution test assertions (20/80 → 35/65)
- [x] 2.2 Add test: "coming up" teaser included when 5+ articles
- [x] 2.3 Add test: "coming up" teaser omitted when fewer than 5 articles
- [x] 2.4 Add test: prompt contains cliffhanger instructions
- [x] 2.5 Add test: prompt contains varied interruption types (excited, skeptical, confused, connecting dots, disagreement)
- [x] 2.6 Add test: prompt contains strict turn-length enforcement

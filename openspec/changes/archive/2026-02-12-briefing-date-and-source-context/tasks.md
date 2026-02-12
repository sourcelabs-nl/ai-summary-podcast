## 1. Add source domain extraction

- [x] 1.1 Add a private helper function to BriefingComposer that extracts the domain name from an article URL (e.g., `https://techcrunch.com/2026/...` → `techcrunch.com`)

## 2. Enrich the summary block with source info

- [x] 2.1 Update the `summaryBlock` construction to include the source domain alongside each article's title and summary (e.g., `1. [techcrunch.com] Article Title\nSummary text`)

## 3. Add date and podcast identity to prompt

- [x] 3.1 Add the current date (formatted as human-readable, e.g., "Wednesday, February 12, 2026") to the prompt context
- [x] 3.2 Add the podcast name and topic to the prompt context

## 4. Update prompt instructions

- [x] 4.1 Add instruction to mention the podcast name, topic, and current date in the introduction
- [x] 4.2 Add instruction to subtly and sparingly attribute information to its source throughout the script

## 5. Testing

- [x] 5.1 Add unit test for the domain extraction helper (normal URLs, edge cases)
- [x] 5.2 Add unit test verifying the summary block includes source domain names

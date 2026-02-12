## 1. Prompt Fix

- [x] 1.1 Update the prohibition line in BriefingComposer prompt to explicitly list bracketed section headers (`[Opening]`, `[Closing]`, `[Transition]`) as prohibited patterns

## 2. Post-Generation Sanitization

- [x] 2.1 Add a private function to BriefingComposer that strips lines consisting solely of bracketed headers (regex: lines matching `^\[.+?]\s*$`)
- [x] 2.2 Call the sanitization function on the LLM response before returning the script

## 3. Testing

- [x] 3.1 Add unit test verifying the sanitization function removes `[Opening]`, `[Transition]`, `[Closing]` lines
- [x] 3.2 Add unit test verifying the sanitization function preserves legitimate inline bracketed text within sentences

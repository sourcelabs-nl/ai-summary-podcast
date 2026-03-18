## 1. InterviewComposer Prompt Updates

- [x] 1.1 Add engagement techniques (hook opening, front-load best story, curiosity hooks, mid-roll callbacks, short segments, natural interruptions, emphasis) to `InterviewComposer.buildPrompt()`
- [x] 1.2 Amend "no stage directions" line to clarify TTS-supported cues ARE allowed inside speaker tags
- [x] 1.3 Update `InterviewComposerTest` to verify new prompt content

## 2. DialogueComposer Prompt Updates

- [x] 2.1 Add engagement techniques (hook opening, front-load best story, curiosity hooks, mid-roll callbacks, short segments, natural interruptions, emphasis) to `DialogueComposer.buildPrompt()`
- [x] 2.2 Amend "no stage directions" line to clarify TTS-supported cues ARE allowed inside speaker tags
- [x] 2.3 Update `DialogueComposerTest` to verify new prompt content

## 3. BriefingComposer Prompt Updates

- [x] 3.1 Add applicable engagement techniques (hook opening, front-load best story, short segments, emphasis) to `BriefingComposer.buildPrompt()`
- [x] 3.2 Amend "no stage directions" line to clarify TTS-supported cues ARE allowed inside speaker tags
- [x] 3.3 Update `BriefingComposerTest` to verify new prompt content

## 4. Fix Nullable Field Clearing in Update Endpoint

- [x] 4.1 Add `orKeep` helper functions for String? and Map? to distinguish "absent" (keep existing) from "empty" (clear to null)
- [x] 4.2 Apply `orKeep` to customInstructions, sponsor, speakerNames, pronunciations, llmModels, ttsSettings in update endpoint
- [x] 4.3 Add tests for clearing nullable fields (empty values) and keeping them (absent values)

## 5. Production Data Cleanup

- [x] 5.1 Clear `customInstructions` on podcast `85b9d107-f608-45be-a8f6-3ed1f731967a` via direct database update

## 6. Verification

- [x] 6.1 Run all composer tests to verify changes pass (78 tests, 0 failures)
- [x] 6.2 Run all podcast controller tests to verify changes pass (14 tests, 0 failures)

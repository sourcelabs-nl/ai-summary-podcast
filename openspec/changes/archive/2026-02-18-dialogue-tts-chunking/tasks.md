## 1. Dialogue TTS Chunking

- [x] 1.1 Add batching logic to `ElevenLabsDialogueTtsProvider`: after mapping turns to `DialogueInput` list, group inputs into batches where each batch's total text length stays under 5000 characters; make one `apiClient.textToDialogue()` call per batch; collect all audio byte arrays; set `requiresConcatenation = batches.size > 1`; log batch count
- [x] 1.2 Add test: short dialogue (under 5000 chars) produces single API call with `requiresConcatenation = false` (existing test covers this, verify it still passes)
- [x] 1.3 Add test: long dialogue (over 5000 chars) is split into multiple batches, each under 5000 chars, with `requiresConcatenation = true` and correct number of audio chunks
- [x] 1.4 Add test: turns are never split across batches — verify batch boundary falls between complete turns

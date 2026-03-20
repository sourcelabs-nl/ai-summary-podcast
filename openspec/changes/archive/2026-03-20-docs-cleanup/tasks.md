## 1. README.md Cleanup

- [x] 1.1 Simplify `.envrc` section: only `APP_ENCRYPTION_MASTER_KEY` is required. Move provider API keys (OpenRouter, OpenAI, ElevenLabs) to a note about optional global fallbacks manageable via dashboard/API. Keep SoundCloud and X credentials as optional with explanation.
- [x] 1.2 Update "Using Ollama" and "Using ElevenLabs" sections: consolidated into a single "Provider Configuration" section, dashboard-first, curl examples removed in favor of API reference links
- [x] 1.3 Fix podcast settings table: update `llmModels` description to mention `{provider, model}` structure and link to Model Configuration section
- [x] 1.4 Update TTS Configuration section: lead with dashboard/API management, mention env vars as fallbacks rather than primary method
- [x] 1.5 Fix "Web Dashboard" section: update podcast settings bullet to mention provider/model dropdowns instead of "key-value editors for JSON map fields"
- [x] 1.6 Review and fix any remaining stale references throughout README (no remaining issues found)

## 2. CLAUDE.md Cleanup

- [x] 2.1 Remove "Project Overview" section: replaced with one-liner referencing README
- [x] 2.2 Remove "Architecture (Four-Stage Pipeline)" section: removed, covered by README
- [x] 2.3 Remove "Data Model" section: removed, covered by README
- [x] 2.4 Remove "External Dependencies" section: removed, covered by README Prerequisites
- [x] 2.5 Remove "Source Configuration" section: removed (was outdated with reddit/youtube/YAML references)
- [x] 2.6 Fix "Running the Application" section: changed required env vars to only `APP_ENCRYPTION_MASTER_KEY`
- [x] 2.7 Review remaining sections for accuracy: all remaining sections are Claude-specific instructions, no issues found

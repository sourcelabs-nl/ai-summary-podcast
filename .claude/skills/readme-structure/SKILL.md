---
name: readme-structure
description: Use when updating README.md to ensure correct section order and structure are maintained.
user-invocable: false
---

# README Structure

When updating `README.md`, maintain the following section order and structure. Do not remove, reorder, or rename sections. New sections may be added at the end before "Running Tests".

1. **Title + description** — `# AI Summary Podcast` followed by a one-paragraph project summary.
2. **How It Works** — Mermaid flowchart of the pipeline stages, followed by a numbered list explaining each stage. Ends with a note on per-user podcast customization.
3. **Prerequisites** — Bulleted list of required tools and services (Java version, FFmpeg, LLM provider options, TTS API key).
4. **Setup** — Step-by-step instructions: `.env` file creation with required variables, key generation command, explanation of fallback vs per-user provider config.
   - **Using Ollama instead of OpenRouter** — Subsection with Ollama-specific setup (pull model, configure user provider via API).
   - **Using ElevenLabs instead of OpenAI for TTS** — Subsection with ElevenLabs-specific setup.
   - Start/stop commands (`./start.sh`, `./stop.sh`) and direct run command (`./mvnw spring-boot:run`).
   - Note on default port and data directory.
5. **Customizing Your Podcast** — Settings table and subsections for styles, TTS config, model config, episode review, cost tracking, cost gate, static feed export, publishing, X/Twitter, Nitter, and example.
6. **API Overview** — Endpoint reference grouped by resource (Users, Podcasts, Episodes, Sources, Publishing, OAuth, Voices, Provider Configuration).
7. **Running Tests** — Test command (`./mvnw test`) and note on MockK usage.

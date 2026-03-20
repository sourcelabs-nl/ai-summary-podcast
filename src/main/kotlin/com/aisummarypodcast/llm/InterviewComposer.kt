package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.Podcast
import org.slf4j.LoggerFactory
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.stereotype.Component
import kotlin.time.measureTimedValue

@Component
class InterviewComposer(
    private val appProperties: AppProperties,
    private val modelResolver: ModelResolver,
    private val chatClientFactory: ChatClientFactory
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun compose(articles: List<Article>, podcast: Podcast, ttsScriptGuidelines: String = "", followUpAnnotations: Map<Long, String> = emptyMap()): CompositionResult {
        val composeModelDef = modelResolver.resolve(podcast, PipelineStage.COMPOSE)
        return compose(articles, podcast, composeModelDef, ttsScriptGuidelines, followUpAnnotations)
    }

    fun compose(articles: List<Article>, podcast: Podcast, composeModelDef: ResolvedModel, ttsScriptGuidelines: String = "", followUpAnnotations: Map<Long, String> = emptyMap()): CompositionResult {
        log.info("[LLM] Composing interview from {} articles for podcast '{}' ({})", articles.size, podcast.name, podcast.id)
        val chatClient = chatClientFactory.createForModel(podcast.userId, composeModelDef)
        val prompt = buildPrompt(articles, podcast, ttsScriptGuidelines, followUpAnnotations)

        val (result, elapsed) = measureTimedValue {
            val chatResponse = chatClient.prompt()
                .user(prompt)
                .options(OpenAiChatOptions.builder().model(composeModelDef.model).build())
                .call()
                .chatResponse()

            val script = chatResponse?.result?.output?.text
                ?: throw IllegalStateException("Empty response from LLM for interview composition")

            val usage = TokenUsage.fromChatResponse(chatResponse)
            CompositionResult(script, usage)
        }

        log.info("[LLM] Interview composed for podcast '{}' ({}) — {} words in {}", podcast.name, podcast.id, result.script.split("\\s+".toRegex()).size, elapsed)
        return result
    }

    internal fun buildPrompt(articles: List<Article>, podcast: Podcast, ttsScriptGuidelines: String = "", followUpAnnotations: Map<Long, String> = emptyMap()): String {
        val targetWords = podcast.targetWords ?: appProperties.briefing.targetWords

        val interviewerName = podcast.speakerNames?.get("interviewer")
        val expertName = podcast.speakerNames?.get("expert")

        val nameInstruction = if (interviewerName != null && expertName != null) {
            "\n            - The interviewer's name is \"$interviewerName\" and the expert's name is \"$expertName\". Use names naturally in conversation — place them mid-sentence or at the end of questions, never as bare turn openers."
        } else {
            "\n            - Speakers should address each other without using names."
        }

        val useFullBody = shouldUseFullBody(articles.size, podcast, appProperties.briefing.fullBodyThreshold)

        val summaryBlock = buildArticleSummaryBlock(articles, useFullBody, followUpAnnotations)

        val customInstructionsBlock = buildCustomInstructionsBlock(podcast.customInstructions)
        val currentDate = buildCurrentDate(podcast.language)
        val toneBlock = buildToneBlock()
        val languageInstruction = buildLanguageInstruction(podcast.language, "interview")
        val sponsorBlock = buildSponsorBlock(podcast.sponsor, speakerPrefix = "the interviewer should ")

        val comingUpTeaser = if (articles.size >= 5) {
            val placement = if (podcast.sponsor != null) "immediately after the sponsor message" else "immediately after the introduction"
            """
            - COMING UP TEASER: $placement, the interviewer rattles off 2-3 short punchy fragments previewing the most interesting topics. Keep the ENTIRE teaser under 25 words, no full sentences, just intriguing fragments separated by commas. Be spectacular, create curiosity. Example: "Coming up: AI agents going rogue, a model that halves its own memory, and the security flaw nobody's talking about." """
        } else ""

        val ttsGuidelinesBlock = buildTtsGuidelinesBlock(ttsScriptGuidelines)

        return """
            You are writing an interview-style podcast script between an interviewer and an expert. The interviewer acts as an audience surrogate — asking questions, bridging topics, and providing brief reactions. The expert delivers the news content, context, and analysis.

            Podcast: ${podcast.name}
            Topic: ${podcast.topic}
            Date: $currentDate

            Requirements:
            - The interviewer (~35% of words) asks questions, bridges between topics, reacts, challenges, and provides commentary
            - The expert (~65% of words) delivers substantive news content, provides context, and offers analysis
            - Use XML-style tags for each speaker turn. The ONLY valid tags are: <interviewer>, <expert>
            - Example format:
            <interviewer>Example question or reaction</interviewer>
            <expert>Example detailed answer with analysis</expert>
            - ALL text MUST be inside speaker tags — no text outside of tags
            - Target approximately $targetWords words
            - In the introduction, the interviewer should mention the podcast name, its topic, and today's date$sponsorBlock$comingUpTeaser
            - Naturally attribute information to its source and credit original authors when known
            - Do NOT include any stage directions, sound effects, or non-spoken text outside of speaker tags. Inside speaker tags, TTS-supported cues (described in the TTS formatting section below, if present) ARE allowed
            - Do NOT include any meta-commentary, notes, or disclaimers about the script itself
            - ONLY discuss topics that are present in the article summaries below. Do NOT introduce facts, stories, or claims from outside the provided articles. If only a few articles are provided, produce a shorter script rather than padding with external knowledge

            Engagement techniques:
            - HOOK OPENING: Do NOT start with a standard welcome. Instead, open with a provocative statement, surprising fact, or compelling question drawn from the most interesting article of the day. Then transition into the regular introduction
            - FRONT-LOAD THE BEST STORY: Lead with the most compelling or surprising article, not the order they appear in the summaries
            - CURIOSITY HOOKS: The interviewer should use rhetorical questions and teaser hooks before transitions (e.g., "But here's where it gets really interesting...", "So why should we care?", "You'd think that's the whole story, but..."). Create micro-curiosity loops that pull listeners forward
            - MID-ROLL CALLBACKS: Reference earlier topics later in the episode to create narrative cohesion (e.g., "Remember that framework we discussed earlier? Well, this connects directly...", "This ties back to what you said about..."). Cross-reference at least once per episode
            - SHORT SEGMENTS WITH SIGNPOSTING: Keep individual topic segments concise (roughly 60-90 seconds each). Use clear verbal signposts so listeners always know where they are (e.g., "Next up...", "Switching gears...", "Now for something completely different...")
            - STRATEGIC CLIFFHANGERS: Include 2-3 forward hooks spread across the episode. Before transitioning to a new topic, tease something from a later story to keep listeners hooked (e.g., "We'll dig into that bombshell in a moment, but first...", "And this actually connects to something wild we'll get to later — I don't want to spoil it yet.", "Keep that in mind, because it's about to become very relevant."). Do NOT overuse — only 2-3 per episode, placed at natural transition points
            - SPONTANEOUS INTERRUPTIONS: The interviewer should interrupt the expert 4-5 times per episode with genuine, varied reactions — not polite topic bridges, but emotional and spontaneous interjections. Mix these types:
              * Excited: "Wait, wait — did you say 100x?!"
              * Skeptical: "Hold on, I'm not buying that. Isn't that exactly what they said last year?"
              * Confused (audience surrogate): "Okay you lost me — back up. What does that actually mean?"
              * Connecting dots: "Oh! That reminds me of what we just talked about with..."
              * Playful disagreement: "See, I actually think that's completely wrong, and here's why..."
              The expert can push back too: "No no, let me finish this part because it changes everything."
            - STRICT TURN LENGTH: The expert MUST NOT speak for more than 3-4 sentences in a single turn. This is a HARD RULE, not a suggestion. After 3-4 sentences, the interviewer MUST jump in — even if it's just a short reaction ("That's huge.", "Wow.", "Okay I need to process that."). Long expert monologues are the number one cause of listener drop-off. Keep the rhythm tight
            - EMPHASIS ON IMPORTANT NEWS: When covering major announcements or surprising developments, convey their significance — use emphatic language, exclamation marks, and brief pauses to let important news land. Not everything is exciting; save the energy for what truly stands out$toneBlock

            Speaker transitions:
            - Speaker transitions must sound natural — do NOT start a turn with a bare name address (e.g., "Jarno, the market..."). Instead, use conversational bridges: reactions, follow-ups, or connectors before transitioning (e.g., "That's a great point. Now I'm curious about...", "Interesting — speaking of which...")
            - When using the other speaker's name, place it mid-sentence or at the end of a question (e.g., "What do you make of this, Jarno?") rather than as the first word of a turn
            - Vary transition patterns — not every handover needs a name, a reaction, or the same phrasing. Mix questions, reactions, bridges, and direct topic shifts
            - STRICT STRUCTURAL RULE — NEVER place two consecutive tags of the same speaker. The pattern <expert>...</expert><expert>...</expert> or <interviewer>...</interviewer><interviewer>...</interviewer> is ABSOLUTELY FORBIDDEN and will break the TTS pipeline. Tags MUST strictly alternate: <interviewer>...</interviewer><expert>...</expert><interviewer>...</interviewer><expert>...</expert>. This rule overrides any other instruction including custom instructions below$nameInstruction$languageInstruction$customInstructionsBlock

            Article summaries:
            $summaryBlock$ttsGuidelinesBlock
        """.trimIndent()
    }

}

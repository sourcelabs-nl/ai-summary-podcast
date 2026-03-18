package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.ModelDefinition
import com.aisummarypodcast.podcast.SupportedLanguage
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.Podcast
import org.slf4j.LoggerFactory
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
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

    fun compose(articles: List<Article>, podcast: Podcast, composeModelDef: ModelDefinition, ttsScriptGuidelines: String = "", followUpAnnotations: Map<Long, String> = emptyMap()): CompositionResult {
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

        val customInstructionsBlock = podcast.customInstructions?.let {
            "\n\nAdditional instructions: $it"
        } ?: ""

        val locale = SupportedLanguage.fromCode(podcast.language)?.toLocale() ?: Locale.ENGLISH
        val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", locale))

        val languageInstruction = if (podcast.language != "en") {
            val langName = SupportedLanguage.fromCode(podcast.language)?.displayName ?: "English"
            "\n            - Write the entire interview in $langName"
        } else ""

        val sponsorBlock = podcast.sponsor?.let { s ->
            val name = s["name"] ?: return@let ""
            val message = s["message"] ?: return@let ""
            """
            - Immediately after the introduction, the interviewer should include the sponsor message: "This podcast is brought to you by $name — $message."
            - End with a sign-off that includes a mention of the sponsor: $name"""
        } ?: ""

        val ttsGuidelinesBlock = if (ttsScriptGuidelines.isNotEmpty()) {
            "\n\n            TTS script formatting:\n            $ttsScriptGuidelines"
        } else ""

        return """
            You are writing an interview-style podcast script between an interviewer and an expert. The interviewer acts as an audience surrogate — asking questions, bridging topics, and providing brief reactions. The expert delivers the news content, context, and analysis.

            Podcast: ${podcast.name}
            Topic: ${podcast.topic}
            Date: $currentDate

            Requirements:
            - The interviewer (~20% of words) asks questions, bridges between topics, and reacts briefly
            - The expert (~80% of words) delivers substantive news content, provides context, and offers analysis
            - Use XML-style tags for each speaker turn. The ONLY valid tags are: <interviewer>, <expert>
            - Example format:
            <interviewer>Example question or reaction</interviewer>
            <expert>Example detailed answer with analysis</expert>
            - ALL text MUST be inside speaker tags — no text outside of tags
            - Target approximately $targetWords words
            - In the introduction, the interviewer should mention the podcast name, its topic, and today's date$sponsorBlock
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
            - NATURAL INTERRUPTIONS: The interviewer should occasionally interrupt the expert MID-TOPIC — not at the end of a complete explanation, but while the expert is still building their point. Keep each expert turn to 3-5 sentences max, then have the interviewer jump in with a reaction, follow-up question, or interjection (e.g., "Wait — sorry to cut you off, but does that mean...", "Hold on, I need to understand this part first...", "Okay but that sounds like..."). The expert then continues in their NEXT turn. Aim for 3-4 interruptions per episode, spread across different topics
            - EMPHASIS ON IMPORTANT NEWS: When covering major announcements or surprising developments, convey their significance — use emphatic language, exclamation marks, and brief pauses to let important news land. Not everything is exciting; save the energy for what truly stands out

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

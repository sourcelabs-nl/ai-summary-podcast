package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.podcast.SupportedLanguage
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PodcastStyle
import org.slf4j.LoggerFactory
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.measureTimedValue

data class CompositionResult(
    val script: String,
    val usage: TokenUsage
)

@Component
class BriefingComposer(
    private val appProperties: AppProperties,
    private val modelResolver: ModelResolver,
    private val chatClientFactory: ChatClientFactory
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val stylePrompts = mapOf(
        PodcastStyle.NEWS_BRIEFING to "You are a professional news anchor creating an audio briefing. Use a structured, authoritative tone with smooth transitions between topics.",
        PodcastStyle.CASUAL to "You are a friendly podcast host having a casual chat. Use a conversational, relaxed tone as if talking to a friend.",
        PodcastStyle.DEEP_DIVE to "You are an analytical podcast host doing a deep-dive exploration. Provide in-depth analysis and thoughtful commentary on each topic.",
        PodcastStyle.EXECUTIVE_SUMMARY to "You are creating a concise executive summary. Be fact-focused with minimal commentary. Get straight to the point."
    )

    fun compose(articles: List<Article>, podcast: Podcast, ttsScriptGuidelines: String = "", followUpAnnotations: Map<Long, String> = emptyMap()): CompositionResult {
        val composeModelDef = modelResolver.resolve(podcast, PipelineStage.COMPOSE)
        return compose(articles, podcast, composeModelDef, ttsScriptGuidelines, followUpAnnotations)
    }

    fun compose(articles: List<Article>, podcast: Podcast, composeModelDef: ResolvedModel, ttsScriptGuidelines: String = "", followUpAnnotations: Map<Long, String> = emptyMap()): CompositionResult {
        log.info("[LLM] Composing briefing from {} articles for podcast '{}' ({}) (style: {})", articles.size, podcast.name, podcast.id, podcast.style)
        val chatClient = chatClientFactory.createForModel(podcast.userId, composeModelDef)
        val prompt = buildPrompt(articles, podcast, ttsScriptGuidelines, followUpAnnotations)

        val (result, elapsed) = measureTimedValue {
            val chatResponse = chatClient.prompt()
                .user(prompt)
                .options(OpenAiChatOptions.builder().model(composeModelDef.model).build())
                .call()
                .chatResponse()

            val rawScript = chatResponse?.result?.output?.text
                ?: throw IllegalStateException("Empty response from LLM for briefing composition")

            val script = stripSectionHeaders(rawScript)
            val usage = TokenUsage.fromChatResponse(chatResponse)
            CompositionResult(script, usage)
        }

        log.info("[LLM] Briefing composed for podcast '{}' ({}) — {} words in {}", podcast.name, podcast.id, result.script.split("\\s+".toRegex()).size, elapsed)
        return result
    }

    internal fun buildPrompt(articles: List<Article>, podcast: Podcast, ttsScriptGuidelines: String = "", followUpAnnotations: Map<Long, String> = emptyMap()): String {
        val targetWords = podcast.targetWords ?: appProperties.briefing.targetWords
        val stylePrompt = stylePrompts[podcast.style] ?: stylePrompts[PodcastStyle.NEWS_BRIEFING]!!

        val useFullBody = shouldUseFullBody(articles.size, podcast, appProperties.briefing.fullBodyThreshold)

        val summaryBlock = buildArticleSummaryBlock(articles, useFullBody, followUpAnnotations)

        val customInstructionsBlock = podcast.customInstructions?.let {
            "\n\nAdditional instructions: $it"
        } ?: ""

        val locale = SupportedLanguage.fromCode(podcast.language)?.toLocale() ?: Locale.ENGLISH
        val today = LocalDate.now()
        val currentDate = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", locale))
        val fridayBlock = buildFridayBlock()

        val languageInstruction = if (podcast.language != "en") {
            val langName = SupportedLanguage.fromCode(podcast.language)?.displayName ?: "English"
            "\n            - Write the entire script in $langName"
        } else ""

        val sponsorBlock = podcast.sponsor?.let { s ->
            val name = s["name"] ?: return@let ""
            val message = s["message"] ?: return@let ""
            """
            - Immediately after the introduction, include the sponsor message: "This podcast is brought to you by $name — $message."
            - End with a sign-off that includes a mention of the sponsor: $name"""
        } ?: ""

        val ttsGuidelinesBlock = if (ttsScriptGuidelines.isNotEmpty()) {
            "\n\n            TTS script formatting:\n            $ttsScriptGuidelines"
        } else ""


        return """
            $stylePrompt

            Compose the following article summaries into a coherent, engaging monologue suitable for a podcast episode.

            Podcast: ${podcast.name}
            Topic: ${podcast.topic}
            Date: $currentDate

            Requirements:
            - Use natural spoken language, not written style
            - Include smooth transitions between topics
            - Target approximately $targetWords words
            - In the introduction, mention the podcast name, its topic, and today's date$sponsorBlock
            - Naturally attribute information to its source and credit original authors when known (e.g., "as John Smith reports for TechCrunch") — do not over-cite
            - Do NOT include any stage directions, sound effects, section headers (like [Opening], [Closing], [Transition]), or non-spoken text. TTS-supported cues (described in the TTS formatting section below, if present) ARE allowed
            - Do NOT include any meta-commentary, notes, or disclaimers about the script itself
            - ONLY discuss topics that are present in the article summaries below. Do NOT introduce facts, stories, or claims from outside the provided articles. If only a few articles are provided, produce a shorter script rather than padding with external knowledge

            Engagement techniques:
            - HOOK OPENING: Do NOT start with a standard welcome. Instead, open with a provocative statement, surprising fact, or compelling question drawn from the most interesting article of the day. Then transition into the regular introduction
            - FRONT-LOAD THE BEST STORY: Lead with the most compelling or surprising article, not the order they appear in the summaries
            - SHORT SEGMENTS WITH SIGNPOSTING: Keep individual topic segments concise. Use clear verbal signposts and smooth transitions so listeners always know where they are
            - EMPHASIS ON IMPORTANT NEWS: When covering major announcements or surprising developments, convey their significance — use emphatic language, exclamation marks, and brief pauses to let important news land. Not everything is exciting; save the energy for what truly stands out$fridayBlock$languageInstruction$customInstructionsBlock

            Article summaries:
            $summaryBlock$ttsGuidelinesBlock
        """.trimIndent()
    }

    internal fun stripSectionHeaders(script: String): String =
        script
            .replace(Regex("(?m)^\\[.+?]\\s*\\n"), "")
            .replace(Regex("\\s*\\((?:Dit script|This script|Note:|Disclaimer:)[^)]*\\)\\s*$"), "")
            .trim()

}

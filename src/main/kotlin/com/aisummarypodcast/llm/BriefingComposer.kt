package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.ModelDefinition
import com.aisummarypodcast.podcast.SupportedLanguage
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.Podcast
import org.slf4j.LoggerFactory
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.stereotype.Component
import java.net.URI
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.measureTimedValue

@Component
class BriefingComposer(
    private val appProperties: AppProperties,
    private val modelResolver: ModelResolver,
    private val chatClientFactory: ChatClientFactory
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val stylePrompts = mapOf(
        "news-briefing" to "You are a professional news anchor creating an audio briefing. Use a structured, authoritative tone with smooth transitions between topics.",
        "casual" to "You are a friendly podcast host having a casual chat. Use a conversational, relaxed tone as if talking to a friend.",
        "deep-dive" to "You are an analytical podcast host doing a deep-dive exploration. Provide in-depth analysis and thoughtful commentary on each topic.",
        "executive-summary" to "You are creating a concise executive summary. Be fact-focused with minimal commentary. Get straight to the point."
    )

    fun compose(articles: List<Article>, podcast: Podcast): String {
        val composeModelDef = modelResolver.resolve(podcast, "compose")
        return compose(articles, podcast, composeModelDef)
    }

    fun compose(articles: List<Article>, podcast: Podcast, composeModelDef: ModelDefinition): String {
        log.info("[LLM] Composing briefing from {} articles (style: {})", articles.size, podcast.style)
        val chatClient = chatClientFactory.createForModel(podcast.userId, composeModelDef)
        val prompt = buildPrompt(articles, podcast)

        val (script, elapsed) = measureTimedValue {
            val rawScript = chatClient.prompt()
                .user(prompt)
                .options(OpenAiChatOptions.builder().model(composeModelDef.model).build())
                .call()
                .content() ?: throw IllegalStateException("Empty response from LLM for briefing composition")

            stripSectionHeaders(rawScript)
        }

        log.info("[LLM] Briefing composed — {} words in {}", script.split("\\s+".toRegex()).size, elapsed)
        return script
    }

    internal fun buildPrompt(articles: List<Article>, podcast: Podcast): String {
        val targetWords = podcast.targetWords ?: appProperties.briefing.targetWords
        val stylePrompt = stylePrompts[podcast.style] ?: stylePrompts["news-briefing"]!!

        val summaryBlock = articles.mapIndexed { index, article ->
            val source = extractDomain(article.url)
            "${index + 1}. [$source] ${article.title}\n${article.summary}"
        }.joinToString("\n\n")

        val customInstructionsBlock = podcast.customInstructions?.let {
            "\n\nAdditional instructions: $it"
        } ?: ""

        val locale = SupportedLanguage.fromCode(podcast.language)?.toLocale() ?: Locale.ENGLISH
        val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", locale))

        val languageInstruction = if (podcast.language != "en") {
            val langName = SupportedLanguage.fromCode(podcast.language)?.displayName ?: "English"
            "\n            - Write the entire script in $langName"
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
            - In the introduction, mention the podcast name, its topic, and today's date
            - Subtly and sparingly attribute information to its source (e.g., "according to TechCrunch") — do not over-cite
            - End with a sign-off
            - Do NOT include any stage directions, sound effects, section headers (like [Opening], [Closing], [Transition]), or non-spoken text
            - Do NOT include any meta-commentary, notes, or disclaimers about the script itself$languageInstruction$customInstructionsBlock

            Article summaries:
            $summaryBlock
        """.trimIndent()
    }

    internal fun stripSectionHeaders(script: String): String =
        script
            .replace(Regex("(?m)^\\[.+?]\\s*\\n"), "")
            .replace(Regex("\\s*\\((?:Dit script|This script|Note:|Disclaimer:)[^)]*\\)\\s*$"), "")
            .trim()

    internal fun extractDomain(url: String): String =
        try {
            URI(url).host?.removePrefix("www.") ?: url
        } catch (_: Exception) {
            url
        }
}

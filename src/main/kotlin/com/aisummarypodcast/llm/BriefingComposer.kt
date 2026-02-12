package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.Podcast
import org.slf4j.LoggerFactory
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.stereotype.Component
import java.net.URI
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Component
class BriefingComposer(
    private val appProperties: AppProperties,
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
        val chatClient = chatClientFactory.createForPodcast(podcast)
        val targetWords = podcast.targetWords ?: appProperties.briefing.targetWords
        val style = podcast.style
        val stylePrompt = stylePrompts[style] ?: stylePrompts["news-briefing"]!!

        val summaryBlock = articles.mapIndexed { index, article ->
            val source = extractDomain(article.url)
            "${index + 1}. [$source] ${article.title}\n${article.summary}"
        }.joinToString("\n\n")

        val customInstructionsBlock = podcast.customInstructions?.let {
            "\n\nAdditional instructions: $it"
        } ?: ""

        val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH))

        val prompt = """
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
            - Do NOT include any stage directions, sound effects, section headers (like [Opening], [Closing], [Transition]), or non-spoken text$customInstructionsBlock

            Article summaries:
            $summaryBlock
        """.trimIndent()

        val promptBuilder = chatClient.prompt()
            .user(prompt)

        val model = podcast.llmModel
        if (model != null) {
            promptBuilder.options(OpenAiChatOptions.builder().model(model).build())
        }

        val rawScript = promptBuilder.call()
            .content() ?: throw IllegalStateException("Empty response from LLM for briefing composition")

        val script = stripSectionHeaders(rawScript)

        log.info("Composed briefing script: {} words from {} articles (style: {})", script.split("\\s+".toRegex()).size, articles.size, style)
        return script
    }

    internal fun stripSectionHeaders(script: String): String =
        script.replace(Regex("(?m)^\\[.+?]\\s*\\n"), "")

    internal fun extractDomain(url: String): String =
        try {
            URI(url).host?.removePrefix("www.") ?: url
        } catch (_: Exception) {
            url
        }
}

package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.Podcast
import org.slf4j.LoggerFactory
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.stereotype.Component

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
            "${index + 1}. ${article.title}\n${article.summary}"
        }.joinToString("\n\n")

        val customInstructionsBlock = podcast.customInstructions?.let {
            "\n\nAdditional instructions: $it"
        } ?: ""

        val prompt = """
            $stylePrompt

            Compose the following article summaries into a coherent, engaging monologue suitable for a podcast episode.

            Requirements:
            - Use natural spoken language, not written style
            - Include smooth transitions between topics
            - Target approximately $targetWords words
            - Start with a brief introduction and end with a sign-off
            - Do NOT include any stage directions, sound effects, or non-spoken text$customInstructionsBlock

            Article summaries:
            $summaryBlock
        """.trimIndent()

        val promptBuilder = chatClient.prompt()
            .user(prompt)

        val model = podcast.llmModel
        if (model != null) {
            promptBuilder.options(OpenAiChatOptions.builder().model(model).build())
        }

        val script = promptBuilder.call()
            .content() ?: throw IllegalStateException("Empty response from LLM for briefing composition")

        log.info("Composed briefing script: {} words from {} articles (style: {})", script.split("\\s+".toRegex()).size, articles.size, style)
        return script
    }
}

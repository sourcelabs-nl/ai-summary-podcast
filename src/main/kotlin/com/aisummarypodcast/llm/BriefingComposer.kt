package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.store.Article
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.stereotype.Component

@Component
class BriefingComposer(
    private val chatClient: ChatClient,
    private val appProperties: AppProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun compose(articles: List<Article>): String {
        val summaryBlock = articles.mapIndexed { index, article ->
            "${index + 1}. ${article.title}\n${article.summary}"
        }.joinToString("\n\n")

        val prompt = """
            You are a podcast host creating an audio briefing. Compose the following article summaries into a coherent, engaging monologue suitable for a podcast episode.

            Requirements:
            - Use natural spoken language, not written style
            - Include smooth transitions between topics
            - Target approximately ${appProperties.briefing.targetWords} words
            - Start with a brief introduction and end with a sign-off
            - Do NOT include any stage directions, sound effects, or non-spoken text

            Article summaries:
            $summaryBlock
        """.trimIndent()

        val script = chatClient.prompt()
            .user(prompt)
            .call()
            .content() ?: throw IllegalStateException("Empty response from LLM for briefing composition")

        log.info("Composed briefing script: {} words from {} articles", script.split("\\s+".toRegex()).size, articles.size)
        return script
    }
}

package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.Podcast
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.stereotype.Component

data class RelevanceResult(val score: Int = 0, val justification: String = "")

@Component
class RelevanceFilter(
    private val articleRepository: ArticleRepository,
    private val appProperties: AppProperties,
    private val chatClientFactory: ChatClientFactory
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun filter(articles: List<Article>, podcast: Podcast): List<Article> {
        val chatClient = chatClientFactory.createForPodcast(podcast)
        val model = podcast.llmModel ?: appProperties.llm.cheapModel

        return articles.mapNotNull { article ->
            try {
                val textPreview = article.body.take(2000)
                val prompt = """
                    You are a relevance filter. Given the topic of interest and an article, rate the article's relevance on a scale of 1-5.

                    Topic of interest: ${podcast.topic}

                    Article title: ${article.title}
                    Article text: $textPreview

                    Respond with a JSON object containing "score" (integer 1-5) and "justification" (one sentence).
                """.trimIndent()

                val result = chatClient.prompt()
                    .user(prompt)
                    .options(
                        OpenAiChatOptions.builder()
                            .model(model)
                            .temperature(0.3)
                            .build()
                    )
                    .call()
                    .entity(RelevanceResult::class.java)

                val isRelevant = result != null && result.score >= 3
                articleRepository.save(article.copy(isRelevant = isRelevant))

                log.info("Article '{}' scored {} - {}", article.title, result?.score, result?.justification)

                if (isRelevant) article.copy(isRelevant = true) else null
            } catch (e: Exception) {
                log.error("Error filtering article '{}': {}", article.title, e.message, e)
                null
            }
        }
    }
}

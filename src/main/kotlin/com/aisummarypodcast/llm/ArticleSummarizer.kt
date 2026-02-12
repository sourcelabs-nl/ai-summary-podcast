package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.Podcast
import org.slf4j.LoggerFactory
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.stereotype.Component

@Component
class ArticleSummarizer(
    private val articleRepository: ArticleRepository,
    private val appProperties: AppProperties,
    private val chatClientFactory: ChatClientFactory
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun summarize(articles: List<Article>, podcast: Podcast): List<Article> {
        val chatClient = chatClientFactory.createForPodcast(podcast)
        val model = podcast.llmModel ?: appProperties.llm.cheapModel

        return articles.map { article ->
            try {
                val prompt = """
                    Summarize the following article in 2-3 sentences. Capture the key information.

                    Title: ${article.title}
                    Text: ${article.body}
                """.trimIndent()

                val summary = chatClient.prompt()
                    .user(prompt)
                    .options(
                        OpenAiChatOptions.builder()
                            .model(model)
                            .temperature(0.3)
                            .build()
                    )
                    .call()
                    .content()

                val updated = article.copy(summary = summary)
                articleRepository.save(updated)
                log.info("Summarized article: '{}'", article.title)
                updated
            } catch (e: Exception) {
                log.error("Error summarizing article '{}': {}", article.title, e.message, e)
                article
            }
        }
    }
}

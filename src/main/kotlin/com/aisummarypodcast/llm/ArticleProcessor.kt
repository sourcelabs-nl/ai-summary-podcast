package com.aisummarypodcast.llm

import com.aisummarypodcast.config.ModelDefinition
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.Podcast
import org.slf4j.LoggerFactory
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.stereotype.Component

data class ArticleProcessingResult(
    val score: Int = 0,
    val justification: String = "",
    val summary: String? = null
)

@Component
class ArticleProcessor(
    private val articleRepository: ArticleRepository,
    private val modelResolver: ModelResolver,
    private val chatClientFactory: ChatClientFactory
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun process(articles: List<Article>, podcast: Podcast): List<Article> {
        val filterModelDef = modelResolver.resolve(podcast, "filter")
        return process(articles, podcast, filterModelDef)
    }

    fun process(articles: List<Article>, podcast: Podcast, filterModelDef: ModelDefinition): List<Article> {
        val chatClient = chatClientFactory.createForModel(podcast.userId, filterModelDef)
        val model = filterModelDef.model

        return articles.mapNotNull { article ->
            try {
                val prompt = """
                    You are a relevance filter and summarizer. Given the topic of interest and an article, rate the article's relevance on a scale of 1-5. If the score is 3 or above, also provide a 2-3 sentence summary capturing the key information.

                    Topic of interest: ${podcast.topic}

                    Article title: ${article.title}
                    Article text: ${article.body}

                    Respond with a JSON object containing "score" (integer 1-5), "justification" (one sentence), and "summary" (2-3 sentences, only if score >= 3, otherwise omit or set to null).
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
                    .entity(ArticleProcessingResult::class.java)

                val isRelevant = result != null && result.score >= 3
                val summary = if (isRelevant) result?.summary else null
                val updated = article.copy(isRelevant = isRelevant, summary = summary)
                articleRepository.save(updated)

                log.info("Article '{}' scored {} - {}", article.title, result?.score, result?.justification)

                if (isRelevant) updated else null
            } catch (e: Exception) {
                log.error("Error processing article '{}': {}", article.title, e.message, e)
                null
            }
        }
    }
}

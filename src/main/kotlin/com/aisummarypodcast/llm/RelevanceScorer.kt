package com.aisummarypodcast.llm

import com.aisummarypodcast.config.ModelDefinition
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.Podcast
import org.slf4j.LoggerFactory
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.stereotype.Component

data class RelevanceScoringResult(
    val score: Int = 0,
    val justification: String = ""
)

@Component
class RelevanceScorer(
    private val articleRepository: ArticleRepository,
    private val modelResolver: ModelResolver,
    private val chatClientFactory: ChatClientFactory
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun score(articles: List<Article>, podcast: Podcast): List<Article> {
        val filterModelDef = modelResolver.resolve(podcast, "filter")
        return score(articles, podcast, filterModelDef)
    }

    fun score(articles: List<Article>, podcast: Podcast, filterModelDef: ModelDefinition): List<Article> {
        val chatClient = chatClientFactory.createForModel(podcast.userId, filterModelDef)
        val model = filterModelDef.model

        return articles.mapNotNull { article ->
            log.info("[LLM] Scoring article {}: '{}'", article.id, article.title)
            try {
                val prompt = """
                    You are a relevance scorer. Given the topic of interest and an article, rate the article's relevance on a scale of 0-10.

                    Topic of interest: ${podcast.topic}

                    Article title: ${article.title}
                    Article text: ${article.body}

                    Respond with a JSON object containing "score" (integer 0-10) and "justification" (one sentence explaining the score).
                """.trimIndent()

                val responseEntity = chatClient.prompt()
                    .user(prompt)
                    .options(
                        OpenAiChatOptions.builder()
                            .model(model)
                            .temperature(0.3)
                            .build()
                    )
                    .call()
                    .responseEntity(RelevanceScoringResult::class.java)

                val result = responseEntity.entity()
                val usage = TokenUsage.fromChatResponse(responseEntity.response())
                val costCents = CostEstimator.estimateLlmCostCents(usage.inputTokens, usage.outputTokens, filterModelDef)

                val score = result?.score ?: 0
                val updated = article.copy(
                    relevanceScore = score,
                    llmInputTokens = (article.llmInputTokens ?: 0) + usage.inputTokens,
                    llmOutputTokens = (article.llmOutputTokens ?: 0) + usage.outputTokens,
                    llmCostCents = CostEstimator.addNullableCosts(article.llmCostCents, costCents)
                )
                articleRepository.save(updated)

                log.info("[LLM] Article '{}' scored {} — {}", article.title, score, result?.justification)
                updated
            } catch (e: Exception) {
                log.error("[LLM] Error scoring article '{}': {}", article.title, e.message, e)
                null
            }
        }
    }

}

package com.aisummarypodcast.llm

import com.aisummarypodcast.config.ModelDefinition
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.Podcast
import org.slf4j.LoggerFactory
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.stereotype.Component

data class ScoreSummarizeResult(
    val relevanceScore: Int = 0,
    val summary: String = ""
)

@Component
class ArticleScoreSummarizer(
    private val articleRepository: ArticleRepository,
    private val chatClientFactory: ChatClientFactory
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun scoreSummarize(articles: List<Article>, podcast: Podcast, filterModelDef: ModelDefinition): List<Article> {
        val chatClient = chatClientFactory.createForModel(podcast.userId, filterModelDef)
        val model = filterModelDef.model

        return articles.mapNotNull { article ->
            log.info("[LLM] Scoring and summarizing article {}: '{}'", article.id, article.title)
            try {
                val prompt = """
                    You are a relevance scorer and summarizer. Given the topic of interest and an article, perform the following:
                    1. Rate the article's relevance to the topic on a scale of 0-10
                    2. Summarize the relevant content in 2-3 sentences, filtering out any irrelevant parts

                    Topic of interest: ${podcast.topic}

                    Article title: ${article.title}
                    Article text: ${article.body}

                    Respond with a JSON object containing:
                    - "relevanceScore" (integer 0-10)
                    - "summary" (2-3 sentences capturing the key relevant information)

                    If the article attributes information to a specific person, organization, or study, preserve that attribution in your summary.
                    If the article is completely irrelevant (score 0-2), you may leave the summary empty.
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
                    .responseEntity(ScoreSummarizeResult::class.java)

                val result = responseEntity.entity()
                val usage = TokenUsage.fromChatResponse(responseEntity.response())
                val costCents = CostEstimator.estimateLlmCostCents(usage.inputTokens, usage.outputTokens, filterModelDef)

                val score = result?.relevanceScore ?: 0
                val summary = result?.summary?.takeIf { it.isNotBlank() }

                val updated = article.copy(
                    relevanceScore = score,
                    summary = summary,
                    llmInputTokens = (article.llmInputTokens ?: 0) + usage.inputTokens,
                    llmOutputTokens = (article.llmOutputTokens ?: 0) + usage.outputTokens,
                    llmCostCents = CostEstimator.addNullableCosts(article.llmCostCents, costCents)
                )
                articleRepository.save(updated)

                log.info("[LLM] Article '{}' scored {} — summary: {} chars", article.title, score, summary?.length ?: 0)
                updated
            } catch (e: Exception) {
                log.error("[LLM] Error scoring/summarizing article '{}': {}", article.title, e.message, e)
                null
            }
        }
    }
}

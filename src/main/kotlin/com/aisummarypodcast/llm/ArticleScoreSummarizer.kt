package com.aisummarypodcast.llm

import com.aisummarypodcast.config.ModelDefinition
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.Podcast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
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

    companion object {
        private const val LONG_ARTICLE_WORD_THRESHOLD = 1500
        private const val MEDIUM_ARTICLE_WORD_THRESHOLD = 500
    }

    private val log = LoggerFactory.getLogger(javaClass)

    fun scoreSummarize(articles: List<Article>, podcast: Podcast, filterModelDef: ModelDefinition, sourceLabels: Map<String, String> = emptyMap()): List<Article> {
        val chatClient = chatClientFactory.createForModel(podcast.userId, filterModelDef)
        val model = filterModelDef.model

        return runBlocking(Dispatchers.IO) {
            supervisorScope {
                articles.map { article ->
                    async {
                        val sourceLabel = sourceLabels[article.sourceId]
                        log.info("[LLM] Scoring and summarizing article {}: '{}' (source: {})", article.id, article.title, sourceLabel ?: article.sourceId)
                        try {
                            val prompt = buildPrompt(article, podcast)

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

                            log.info("[LLM] Article '{}' scored {} — summary: {} chars (source: {})", article.title, score, summary?.length ?: 0, sourceLabel ?: article.sourceId)
                            updated
                        } catch (e: Exception) {
                            log.error("[LLM] Error scoring/summarizing article '{}' (source: {}): {}", article.title, sourceLabels[article.sourceId] ?: article.sourceId, e.message, e)
                            null
                        }
                    }
                }.awaitAll().filterNotNull()
            }
        }
    }

    internal fun buildPrompt(article: Article, podcast: Podcast): String {
        val isAggregated = article.title.startsWith("Posts from")
        val authorContext = article.author?.let { "by $it" } ?: ""

        val contentBlock = if (isAggregated) {
            val postContext = if (authorContext.isNotEmpty()) {
                "The following content consists of multiple social media posts $authorContext."
            } else {
                "The following content consists of multiple social media posts."
            }
            "$postContext\n\n${article.body}"
        } else {
            val titleLine = "Content title: ${article.title}"
            val authorLine = if (authorContext.isNotEmpty()) "\nContent author: ${article.author}" else ""
            "$titleLine$authorLine\nContent: ${article.body}"
        }

        val wordCount = article.body.split("\\s+".toRegex()).size
        val summaryLengthInstruction = when {
            wordCount >= LONG_ARTICLE_WORD_THRESHOLD -> "a full paragraph covering key points, context, and attribution"
            wordCount >= MEDIUM_ARTICLE_WORD_THRESHOLD -> "4-6 sentences"
            else -> "2-3 sentences"
        }

        return """
            You are a relevance scorer and summarizer. Given the topic of interest and content, perform the following:
            1. Rate the content's relevance to the topic on a scale of 0-10
            2. Summarize the relevant information in $summaryLengthInstruction, filtering out any irrelevant parts

            Write directly about what happened — say "Anthropic launched X" not "The article discusses Anthropic launching X".

            Topic of interest: ${podcast.topic}

            $contentBlock

            Respond with a JSON object containing:
            - "relevanceScore" (integer 0-10)
            - "summary" ($summaryLengthInstruction of direct, factual statements about the key relevant information)

            If the content attributes information to a specific person, organization, or study, preserve that attribution in your summary.
            If the content is completely irrelevant (score 0-2), you may leave the summary empty.
        """.trimIndent()
    }
}

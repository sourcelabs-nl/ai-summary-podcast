package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.ModelDefinition
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.Podcast
import org.slf4j.LoggerFactory
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.stereotype.Component

data class SummarizationResult(
    val summary: String = ""
)

@Component
class ArticleSummarizer(
    private val articleRepository: ArticleRepository,
    private val modelResolver: ModelResolver,
    private val chatClientFactory: ChatClientFactory,
    private val appProperties: AppProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun summarize(articles: List<Article>, podcast: Podcast): List<Article> {
        val filterModelDef = modelResolver.resolve(podcast, "filter")
        return summarize(articles, podcast, filterModelDef)
    }

    fun summarize(articles: List<Article>, podcast: Podcast, filterModelDef: ModelDefinition): List<Article> {
        val minWords = appProperties.llm.summarizationMinWords
        val chatClient by lazy { chatClientFactory.createForModel(podcast.userId, filterModelDef) }
        val model = filterModelDef.model

        return articles.map { article ->
            val wordCount = article.body.split("\\s+".toRegex()).size
            if (wordCount < minWords) {
                log.info("[LLM] Skipping summarization for '{}' ({} words < {} threshold)", article.title, wordCount, minWords)
                return@map article
            }

            log.info("[LLM] Summarizing article '{}' ({} words)", article.title, wordCount)
            try {
                val prompt = """
                    You are a summarizer. Given an article, provide a 2-3 sentence summary capturing the key information.

                    Article title: ${article.title}
                    Article text: ${article.body}

                    Respond with a JSON object containing "summary" (2-3 sentences capturing the key information).
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
                    .entity(SummarizationResult::class.java)

                val updated = article.copy(summary = result?.summary)
                articleRepository.save(updated)

                log.info("[LLM] Summarized '{}' — {} chars", article.title, result?.summary?.length ?: 0)
                updated
            } catch (e: Exception) {
                log.error("[LLM] Error summarizing article '{}': {}", article.title, e.message, e)
                article
            }
        }
    }
}

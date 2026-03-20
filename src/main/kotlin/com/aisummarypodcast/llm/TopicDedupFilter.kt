package com.aisummarypodcast.llm

import com.aisummarypodcast.store.Article
import org.slf4j.LoggerFactory
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.stereotype.Component
import kotlin.time.measureTimedValue

data class DedupCandidate(
    val id: Long,
    val title: String,
    val summary: String?
)

data class DedupCluster(
    val topic: String = "",
    val status: String = "NEW",
    val previousContext: String? = null,
    val candidateArticleIds: List<Int> = emptyList(),
    val selectedArticleIds: List<Int> = emptyList()
)

data class DedupResult(
    val clusters: List<DedupCluster> = emptyList()
)

data class FilteredArticle(
    val article: Article,
    val followUpContext: String? = null
)

data class DedupFilterResult(
    val filteredArticles: List<FilteredArticle>,
    val usage: TokenUsage
)

@Component
class TopicDedupFilter(
    private val chatClientFactory: ChatClientFactory
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun filter(
        candidates: List<Article>,
        historicalArticles: List<Article>,
        userId: String,
        filterModelDef: ResolvedModel
    ): DedupFilterResult {
        if (candidates.isEmpty()) {
            return DedupFilterResult(emptyList(), TokenUsage(0, 0))
        }

        log.info("[Dedup] Filtering {} candidates against {} historical articles", candidates.size, historicalArticles.size)
        val chatClient = chatClientFactory.createForModel(userId, filterModelDef)
        val prompt = buildPrompt(candidates, historicalArticles)

        val (result, elapsed) = measureTimedValue {
            val chatResponse = chatClient.prompt()
                .user(prompt)
                .options(OpenAiChatOptions.builder().model(filterModelDef.model).temperature(0.3).build())
                .call()
                .responseEntity(DedupResult::class.java)

            val dedupResult = chatResponse.entity()
                ?: throw IllegalStateException("Empty response from LLM for topic dedup filter")

            val usage = TokenUsage.fromChatResponse(chatResponse.response())
            Pair(dedupResult, usage)
        }

        val (dedupResult, usage) = result
        val candidateById = candidates.associateBy { it.id!!.toInt() }
        val filteredArticles = mutableListOf<FilteredArticle>()

        for (cluster in dedupResult.clusters) {
            if (cluster.selectedArticleIds.isEmpty()) continue

            val followUpContext = if (cluster.status == "CONTINUATION" && cluster.previousContext != null) {
                cluster.previousContext
            } else null

            for (articleIndex in cluster.selectedArticleIds) {
                val article = candidateById[articleIndex]
                if (article != null) {
                    filteredArticles.add(FilteredArticle(article, followUpContext))
                }
            }
        }

        log.info("[Dedup] Filter complete in {} — {} candidates → {} selected across {} clusters",
            elapsed, candidates.size, filteredArticles.size, dedupResult.clusters.size)

        return DedupFilterResult(filteredArticles, usage)
    }

    internal fun buildPrompt(candidates: List<Article>, historicalArticles: List<Article>): String {
        val candidateBlock = candidates.mapIndexed { _, article ->
            "${article.id}. [${extractDomain(article.url)}] ${article.title}\n${article.summary ?: article.body}"
        }.joinToString("\n\n")

        val historicalBlock = if (historicalArticles.isNotEmpty()) {
            val grouped = historicalArticles.joinToString("\n") { article ->
                "- [${extractDomain(article.url)}] ${article.title}: ${article.summary ?: "(no summary)"}"
            }
            """

            Historical articles from recent episodes:
            $grouped
            """
        } else ""

        return """
            You are a topic deduplication filter for a podcast pipeline. Your job is to cluster today's candidate articles by topic, compare against historical articles from recent episodes, and decide what's new vs. already covered.

            For each cluster of related articles, output:
            - "topic": short label for the topic
            - "status": "NEW" (not covered in recent episodes) or "CONTINUATION" (covered before)
            - "previousContext": (CONTINUATION only) one sentence describing what was covered before
            - "candidateArticleIds": list of candidate article IDs in this cluster
            - "selectedArticleIds": article IDs to keep for composition (max 3 per cluster)

            Rules:
            - CONTINUATION topics with NO genuinely new information: set selectedArticleIds to empty []
            - CONTINUATION topics WITH new developments: select up to 3 articles with the new information
            - NEW topics with 3 or fewer articles: keep all
            - NEW topics with more than 3 articles: select the 3 most comprehensive/complementary articles (prefer different sources, different angles)
            - Merge cross-source duplicates into one cluster (e.g., TechCrunch and The Verge covering the same announcement)
            - High-scoring single-source articles are likely unique — don't cluster them with loosely related topics
            - Every candidate article must appear in exactly one cluster

            Respond with a JSON object: { "clusters": [ ... ] }

            Today's candidate articles:
            $candidateBlock
            $historicalBlock
        """.trimIndent()
    }

    private fun extractDomain(url: String): String {
        return try {
            java.net.URI(url).host?.removePrefix("www.") ?: url
        } catch (_: Exception) {
            url
        }
    }
}

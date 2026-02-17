package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.source.SourceAggregator
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PostRepository
import com.aisummarypodcast.store.SourceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.time.measureTimedValue

data class PipelineResult(
    val script: String,
    val filterModel: String,
    val composeModel: String,
    val llmInputTokens: Int = 0,
    val llmOutputTokens: Int = 0,
    val llmCostCents: Int? = null,
    val processedArticleIds: List<Long> = emptyList()
)

@Component
class LlmPipeline(
    private val articleScoreSummarizer: ArticleScoreSummarizer,
    private val briefingComposer: BriefingComposer,
    private val modelResolver: ModelResolver,
    private val articleRepository: ArticleRepository,
    private val sourceRepository: SourceRepository,
    private val postRepository: PostRepository,
    private val sourceAggregator: SourceAggregator,
    private val appProperties: AppProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun run(podcast: Podcast): PipelineResult? {
        val sources = sourceRepository.findByPodcastId(podcast.id)
        val sourceIds = sources.map { it.id }
        if (sourceIds.isEmpty()) {
            log.info("[LLM] Podcast {} has no sources — skipping", podcast.id)
            return null
        }

        val filterModelDef = modelResolver.resolve(podcast, "filter")
        val composeModelDef = modelResolver.resolve(podcast, "compose")
        val threshold = podcast.relevanceThreshold

        // Step 1: Aggregate unlinked posts into articles
        val effectiveMaxArticleAgeDays = podcast.maxArticleAgeDays ?: appProperties.source.maxArticleAgeDays
        val cutoff = Instant.now().minus(effectiveMaxArticleAgeDays.toLong(), ChronoUnit.DAYS).toString()
        val unlinkedPosts = postRepository.findUnlinkedBySourceIds(sourceIds, cutoff)

        if (unlinkedPosts.isNotEmpty()) {
            log.info("[LLM] Aggregating {} unlinked posts for podcast {}", unlinkedPosts.size, podcast.id)
            val postsBySource = unlinkedPosts.groupBy { it.sourceId }
            for ((sourceId, posts) in postsBySource) {
                val source = sources.first { it.id == sourceId }
                sourceAggregator.aggregateAndPersist(posts, source)
            }
        }

        // Cost gate: estimate cost before any LLM calls
        val allUnscored = articleRepository.findUnscoredBySourceIds(sourceIds)
        if (allUnscored.isNotEmpty()) {
            val targetWords = podcast.targetWords ?: appProperties.briefing.targetWords
            val estimatedCostCents = CostEstimator.estimatePipelineCostCents(
                allUnscored, filterModelDef, composeModelDef, targetWords
            )
            val costThreshold = podcast.maxLlmCostCents ?: appProperties.llm.maxCostCents
            if (estimatedCostCents == null) {
                log.warn("[LLM] Cost estimation unavailable for podcast {} — pricing not configured for model(s), skipping cost gate", podcast.id)
            } else if (estimatedCostCents > costThreshold) {
                log.warn("[LLM] Cost gate triggered for podcast {}: estimated {}¢ exceeds threshold {}¢ — skipping pipeline", podcast.id, estimatedCostCents, costThreshold)
                return null
            } else {
                log.info("[LLM] Cost gate passed for podcast {}: estimated {}¢ within threshold {}¢", podcast.id, estimatedCostCents, costThreshold)
            }
        }

        // Step 2: Score and summarize unscored articles
        val unscored = allUnscored
        if (unscored.isNotEmpty()) {
            log.info("[LLM] Scoring and summarizing {} articles for podcast {}", unscored.size, podcast.id)
            val (_, scoringDuration) = measureTimedValue {
                articleScoreSummarizer.scoreSummarize(unscored, podcast, filterModelDef)
            }
            log.info("[LLM] Score+summarize complete — {} articles in {}", unscored.size, scoringDuration)
        }

        // Step 3: Compose briefing from all relevant unprocessed articles
        val toCompose = articleRepository.findRelevantUnprocessedBySourceIds(sourceIds, threshold)
        if (toCompose.isEmpty()) {
            log.info("[LLM] No relevant unprocessed articles for podcast {} — skipping briefing generation", podcast.id)
            return null
        }

        val compositionResult = briefingComposer.compose(toCompose, podcast, composeModelDef)

        val processedArticleIds = toCompose.mapNotNull { it.id }

        for (article in toCompose) {
            articleRepository.save(article.copy(isProcessed = true))
        }

        val costCents = CostEstimator.estimateLlmCostCents(
            compositionResult.usage.inputTokens, compositionResult.usage.outputTokens, composeModelDef
        )

        log.info("[LLM] Pipeline complete for podcast {}: {} articles processed into briefing", podcast.id, toCompose.size)
        return PipelineResult(
            script = compositionResult.script,
            filterModel = filterModelDef.model,
            composeModel = composeModelDef.model,
            llmInputTokens = compositionResult.usage.inputTokens,
            llmOutputTokens = compositionResult.usage.outputTokens,
            llmCostCents = costCents,
            processedArticleIds = processedArticleIds
        )
    }
}

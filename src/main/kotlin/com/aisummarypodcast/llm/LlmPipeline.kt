package com.aisummarypodcast.llm

import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.SourceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import kotlin.time.measureTimedValue

data class PipelineResult(
    val script: String,
    val filterModel: String,
    val composeModel: String,
    val llmInputTokens: Int = 0,
    val llmOutputTokens: Int = 0,
    val llmCostCents: Int? = null
)

@Component
class LlmPipeline(
    private val relevanceScorer: RelevanceScorer,
    private val articleSummarizer: ArticleSummarizer,
    private val briefingComposer: BriefingComposer,
    private val modelResolver: ModelResolver,
    private val articleRepository: ArticleRepository,
    private val sourceRepository: SourceRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun run(podcast: Podcast): PipelineResult? {
        val sourceIds = sourceRepository.findByPodcastId(podcast.id).map { it.id }
        if (sourceIds.isEmpty()) {
            log.info("[LLM] Podcast {} has no sources — skipping", podcast.id)
            return null
        }

        val filterModelDef = modelResolver.resolve(podcast, "filter")
        val composeModelDef = modelResolver.resolve(podcast, "compose")
        val threshold = podcast.relevanceThreshold

        // Stage 1: Score unscored articles
        val unscored = articleRepository.findUnscoredBySourceIds(sourceIds)
        if (unscored.isNotEmpty()) {
            log.info("[LLM] Scoring {} articles for podcast {}", unscored.size, podcast.id)
            val (_, scoringDuration) = measureTimedValue {
                relevanceScorer.score(unscored, podcast, filterModelDef)
            }
            log.info("[LLM] Scoring complete — {} articles in {}", unscored.size, scoringDuration)
        }

        // Stage 2: Summarize relevant unsummarized articles (word count filtering done in ArticleSummarizer)
        val unsummarized = articleRepository.findRelevantUnsummarizedBySourceIds(sourceIds, threshold)
        if (unsummarized.isNotEmpty()) {
            log.info("[LLM] Summarizing {} relevant articles for podcast {}", unsummarized.size, podcast.id)
            val (_, summarizationDuration) = measureTimedValue {
                articleSummarizer.summarize(unsummarized, podcast, filterModelDef)
            }
            log.info("[LLM] Summarization complete — {} articles in {}", unsummarized.size, summarizationDuration)
        }

        // Stage 3: Compose briefing from all relevant unprocessed articles
        val toCompose = articleRepository.findRelevantUnprocessedBySourceIds(sourceIds, threshold)
        if (toCompose.isEmpty()) {
            log.info("[LLM] No relevant unprocessed articles for podcast {} — skipping briefing generation", podcast.id)
            return null
        }

        val compositionResult = briefingComposer.compose(toCompose, podcast, composeModelDef)

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
            llmCostCents = costCents
        )
    }
}

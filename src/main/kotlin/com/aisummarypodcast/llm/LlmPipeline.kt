package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.source.SourceAggregator
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PodcastStyle
import com.aisummarypodcast.store.PostRepository
import com.aisummarypodcast.store.SourceRepository
import com.aisummarypodcast.tts.TtsProviderFactory
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

data class PreviewResult(
    val script: String,
    val articleIds: List<Long>
)

@Component
class LlmPipeline(
    private val articleScoreSummarizer: ArticleScoreSummarizer,
    private val briefingComposer: BriefingComposer,
    private val dialogueComposer: DialogueComposer,
    private val interviewComposer: InterviewComposer,
    private val modelResolver: ModelResolver,
    private val articleRepository: ArticleRepository,
    private val sourceRepository: SourceRepository,
    private val postRepository: PostRepository,
    private val sourceAggregator: SourceAggregator,
    private val appProperties: AppProperties,
    private val episodeRepository: EpisodeRepository,
    private val ttsProviderFactory: TtsProviderFactory
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun run(podcast: Podcast, onProgress: (stage: String, detail: Map<String, Any>) -> Unit = { _, _ -> }): PipelineResult? {
        val sources = sourceRepository.findByPodcastId(podcast.id)
        val sourceIds = sources.map { it.id }
        if (sourceIds.isEmpty()) {
            log.info("[LLM] Podcast '{}' ({}) has no sources — skipping", podcast.name, podcast.id)
            return null
        }

        val filterModelDef = modelResolver.resolve(podcast, PipelineStage.FILTER)
        val composeModelDef = modelResolver.resolve(podcast, PipelineStage.COMPOSE)
        val threshold = podcast.relevanceThreshold
        val sourceLabels = sources.associate { it.id to extractDomainAndPath(it.url) }

        // Step 1: Aggregate unlinked posts into articles
        val effectiveMaxArticleAgeDays = podcast.maxArticleAgeDays ?: appProperties.source.maxArticleAgeDays
        val cutoff = Instant.now().minus(effectiveMaxArticleAgeDays.toLong(), ChronoUnit.DAYS).toString()
        val unlinkedPosts = postRepository.findUnlinkedBySourceIds(sourceIds, cutoff)

        if (unlinkedPosts.isNotEmpty()) {
            onProgress("aggregating", mapOf("postCount" to unlinkedPosts.size))
            log.info("[LLM] Aggregating {} unlinked posts for podcast '{}' ({})", unlinkedPosts.size, podcast.name, podcast.id)
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
                log.warn("[LLM] Cost estimation unavailable for podcast '{}' ({}) — pricing not configured for model(s), skipping cost gate", podcast.name, podcast.id)
            } else if (estimatedCostCents > costThreshold) {
                log.warn("[LLM] Cost gate triggered for podcast '{}' ({}): estimated {}¢ exceeds threshold {}¢ — skipping pipeline", podcast.name, podcast.id, estimatedCostCents, costThreshold)
                return null
            } else {
                log.info("[LLM] Cost gate passed for podcast '{}' ({}): estimated {}¢ within threshold {}¢", podcast.name, podcast.id, estimatedCostCents, costThreshold)
            }
        }

        // Step 2: Score and summarize unscored articles
        val unscored = allUnscored
        if (unscored.isNotEmpty()) {
            onProgress("scoring", mapOf("articleCount" to unscored.size))
            log.info("[LLM] Scoring and summarizing {} articles for podcast '{}' ({})", unscored.size, podcast.name, podcast.id)
            val (scoredArticles, scoringDuration) = measureTimedValue {
                articleScoreSummarizer.scoreSummarize(unscored, podcast, filterModelDef, sourceLabels)
            }
            val relevantCount = scoredArticles.count { (it.relevanceScore ?: 0) >= threshold }
            log.info("[LLM] Score+summarize complete — {} articles in {} ({} relevant)", unscored.size, scoringDuration, relevantCount)
        }

        // Step 3: Compose briefing from all relevant unprocessed articles
        val toCompose = articleRepository.findRelevantUnprocessedBySourceIds(sourceIds, threshold)
        if (toCompose.isEmpty()) {
            log.info("[LLM] No relevant unprocessed articles for podcast '{}' ({}) — skipping briefing generation", podcast.name, podcast.id)
            return null
        }

        onProgress("composing", mapOf("articleCount" to toCompose.size))

        // Fetch previous episode recap for continuity context
        val previousRecap = episodeRepository.findMostRecentByPodcastId(podcast.id)?.recap
        if (previousRecap != null) {
            log.info("[LLM] Previous episode recap found for podcast '{}' ({}) — passing to composer", podcast.name, podcast.id)
        } else {
            log.info("[LLM] No previous episode recap for podcast '{}' ({}) — composing without continuity context", podcast.name, podcast.id)
        }

        val ttsProvider = ttsProviderFactory.resolve(podcast)
        val ttsScriptGuidelines = ttsProvider.scriptGuidelines(podcast.style, podcast.pronunciations ?: emptyMap())

        val compositionResult = when (podcast.style) {
            PodcastStyle.DIALOGUE -> dialogueComposer.compose(toCompose, podcast, composeModelDef, previousRecap, ttsScriptGuidelines)
            PodcastStyle.INTERVIEW -> interviewComposer.compose(toCompose, podcast, composeModelDef, previousRecap, ttsScriptGuidelines)
            else -> briefingComposer.compose(toCompose, podcast, composeModelDef, previousRecap, ttsScriptGuidelines)
        }

        val processedArticleIds = toCompose.mapNotNull { it.id }

        for (article in toCompose) {
            articleRepository.save(article.copy(isProcessed = true))
        }

        val costCents = CostEstimator.estimateLlmCostCents(
            compositionResult.usage.inputTokens, compositionResult.usage.outputTokens, composeModelDef
        )

        log.info("[LLM] Pipeline complete for podcast '{}' ({}): {} articles processed into briefing", podcast.name, podcast.id, toCompose.size)
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

    fun recompose(articles: List<Article>, podcast: Podcast): PipelineResult {
        val composeModelDef = modelResolver.resolve(podcast, PipelineStage.COMPOSE)
        val previousRecap = episodeRepository.findMostRecentByPodcastId(podcast.id)?.recap
        val ttsProvider = ttsProviderFactory.resolve(podcast)
        val ttsScriptGuidelines = ttsProvider.scriptGuidelines(podcast.style, podcast.pronunciations ?: emptyMap())

        val compositionResult = when (podcast.style) {
            PodcastStyle.DIALOGUE -> dialogueComposer.compose(articles, podcast, composeModelDef, previousRecap, ttsScriptGuidelines)
            PodcastStyle.INTERVIEW -> interviewComposer.compose(articles, podcast, composeModelDef, previousRecap, ttsScriptGuidelines)
            else -> briefingComposer.compose(articles, podcast, composeModelDef, previousRecap, ttsScriptGuidelines)
        }

        val filterModelDef = modelResolver.resolve(podcast, PipelineStage.FILTER)
        val costCents = CostEstimator.estimateLlmCostCents(
            compositionResult.usage.inputTokens, compositionResult.usage.outputTokens, composeModelDef
        )

        log.info("[LLM] Recompose complete for podcast '{}' ({}): {} articles", podcast.name, podcast.id, articles.size)
        return PipelineResult(
            script = compositionResult.script,
            filterModel = filterModelDef.model,
            composeModel = composeModelDef.model,
            llmInputTokens = compositionResult.usage.inputTokens,
            llmOutputTokens = compositionResult.usage.outputTokens,
            llmCostCents = costCents,
            processedArticleIds = articles.mapNotNull { it.id }
        )
    }

    fun preview(podcast: Podcast, onProgress: (stage: String, detail: Map<String, Any>) -> Unit = { _, _ -> }): PreviewResult? {
        val sources = sourceRepository.findByPodcastId(podcast.id)
        val sourceIds = sources.map { it.id }
        if (sourceIds.isEmpty()) return null

        val filterModelDef = modelResolver.resolve(podcast, PipelineStage.FILTER)
        val composeModelDef = modelResolver.resolve(podcast, PipelineStage.COMPOSE)
        val threshold = podcast.relevanceThreshold
        val sourceLabels = sources.associate { it.id to extractDomainAndPath(it.url) }

        // Step 1: Aggregate unlinked posts into articles
        val effectiveMaxArticleAgeDays = podcast.maxArticleAgeDays ?: appProperties.source.maxArticleAgeDays
        val cutoff = Instant.now().minus(effectiveMaxArticleAgeDays.toLong(), ChronoUnit.DAYS).toString()
        val unlinkedPosts = postRepository.findUnlinkedBySourceIds(sourceIds, cutoff)

        if (unlinkedPosts.isNotEmpty()) {
            onProgress("aggregating", mapOf("postCount" to unlinkedPosts.size))
            log.info("[LLM Preview] Aggregating {} unlinked posts for podcast '{}' ({})", unlinkedPosts.size, podcast.name, podcast.id)
            val postsBySource = unlinkedPosts.groupBy { it.sourceId }
            for ((sourceId, posts) in postsBySource) {
                val source = sources.first { it.id == sourceId }
                sourceAggregator.aggregateAndPersist(posts, source)
            }
        }

        // Step 2: Score unscored articles (persists scores)
        val unscored = articleRepository.findUnscoredBySourceIds(sourceIds)
        if (unscored.isNotEmpty()) {
            onProgress("scoring", mapOf("articleCount" to unscored.size))
            log.info("[LLM Preview] Scoring {} articles for podcast '{}' ({})", unscored.size, podcast.name, podcast.id)
            articleScoreSummarizer.scoreSummarize(unscored, podcast, filterModelDef, sourceLabels)
        }

        // Step 3: Compose script from relevant unprocessed articles (NO marking as processed)
        val toCompose = articleRepository.findRelevantUnprocessedBySourceIds(sourceIds, threshold)
        if (toCompose.isEmpty()) {
            log.info("[LLM Preview] No relevant unprocessed articles for podcast '{}' ({})", podcast.name, podcast.id)
            return null
        }

        onProgress("composing", mapOf("articleCount" to toCompose.size))

        val previousRecap = episodeRepository.findMostRecentByPodcastId(podcast.id)?.recap
        val ttsProvider = ttsProviderFactory.resolve(podcast)
        val ttsScriptGuidelines = ttsProvider.scriptGuidelines(podcast.style, podcast.pronunciations ?: emptyMap())

        val compositionResult = when (podcast.style) {
            PodcastStyle.DIALOGUE -> dialogueComposer.compose(toCompose, podcast, composeModelDef, previousRecap, ttsScriptGuidelines)
            PodcastStyle.INTERVIEW -> interviewComposer.compose(toCompose, podcast, composeModelDef, previousRecap, ttsScriptGuidelines)
            else -> briefingComposer.compose(toCompose, podcast, composeModelDef, previousRecap, ttsScriptGuidelines)
        }

        log.info("[LLM Preview] Preview complete for podcast '{}' ({}): {} articles composed", podcast.name, podcast.id, toCompose.size)
        return PreviewResult(
            script = compositionResult.script,
            articleIds = toCompose.mapNotNull { it.id }
        )
    }
}

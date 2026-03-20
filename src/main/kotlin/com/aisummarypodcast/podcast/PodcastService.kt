package com.aisummarypodcast.podcast

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.llm.LlmPipeline
import com.aisummarypodcast.llm.PreviewResult
import com.aisummarypodcast.source.SourceAggregator
import com.aisummarypodcast.store.*
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

data class GenerateBriefingResult(
    val episode: Episode?,
    val failed: Boolean = false,
    val errorMessage: String? = null
)

data class UpcomingContent(
    val articles: List<Article>,
    val unlinkedPosts: List<Post>,
    val sources: List<Source>,
    val totalPostCount: Long,
    val effectiveArticleCount: Long
)

@Service
class PodcastService(
    private val podcastRepository: PodcastRepository,
    private val sourceRepository: SourceRepository,
    private val articleRepository: ArticleRepository,
    private val postRepository: PostRepository,
    private val postArticleRepository: PostArticleRepository,
    private val episodeArticleRepository: EpisodeArticleRepository,
    private val episodeRepository: EpisodeRepository,
    private val appProperties: AppProperties,
    private val llmPipeline: LlmPipeline,
    private val episodeService: EpisodeService,
    private val eventPublisher: ApplicationEventPublisher,
    private val sourceAggregator: SourceAggregator
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun create(userId: String, name: String, topic: String, podcast: Podcast? = null): Podcast {
        val newPodcast = Podcast(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = name,
            topic = topic,
            language = podcast?.language ?: "en",
            llmModels = podcast?.llmModels,
            ttsProvider = podcast?.ttsProvider ?: TtsProviderType.OPENAI,
            ttsVoices = podcast?.ttsVoices,
            ttsSettings = podcast?.ttsSettings,
            style = podcast?.style ?: PodcastStyle.NEWS_BRIEFING,
            targetWords = podcast?.targetWords,
            cron = podcast?.cron ?: "0 0 6 * * *",
            customInstructions = podcast?.customInstructions,
            relevanceThreshold = podcast?.relevanceThreshold ?: 5,
            requireReview = podcast?.requireReview ?: false,
            maxLlmCostCents = podcast?.maxLlmCostCents,
            maxArticleAgeDays = podcast?.maxArticleAgeDays,
            speakerNames = podcast?.speakerNames,
            fullBodyThreshold = podcast?.fullBodyThreshold,
            sponsor = podcast?.sponsor,
            pronunciations = podcast?.pronunciations,
            recapLookbackEpisodes = podcast?.recapLookbackEpisodes
        )
        return podcastRepository.save(newPodcast)
    }

    fun findByUserId(userId: String): List<Podcast> = podcastRepository.findByUserId(userId)

    fun findById(podcastId: String): Podcast? = podcastRepository.findById(podcastId).orElse(null)

    fun update(podcastId: String, updates: Podcast): Podcast? {
        val existing = findById(podcastId) ?: return null
        val updated = existing.copy(
            name = updates.name,
            topic = updates.topic,
            language = updates.language,
            llmModels = updates.llmModels,
            ttsProvider = updates.ttsProvider,
            ttsVoices = updates.ttsVoices,
            ttsSettings = updates.ttsSettings,
            style = updates.style,
            targetWords = updates.targetWords,
            cron = updates.cron,
            customInstructions = updates.customInstructions,
            relevanceThreshold = updates.relevanceThreshold,
            requireReview = updates.requireReview,
            maxLlmCostCents = updates.maxLlmCostCents,
            maxArticleAgeDays = updates.maxArticleAgeDays,
            speakerNames = updates.speakerNames,
            fullBodyThreshold = updates.fullBodyThreshold,
            sponsor = updates.sponsor,
            pronunciations = updates.pronunciations,
            recapLookbackEpisodes = updates.recapLookbackEpisodes
        )
        return podcastRepository.save(updated)
    }

    fun previewBriefing(podcast: Podcast, onProgress: (stage: String, detail: Map<String, Any>) -> Unit = { _, _ -> }): PreviewResult? {
        return llmPipeline.preview(podcast, onProgress)
    }

    fun generateBriefing(podcast: Podcast): GenerateBriefingResult {
        if (podcast.requireReview && episodeService.hasPendingOrApprovedEpisode(podcast.id)) {
            log.info("Podcast '{}' ({}) has a pending/approved episode — skipping generation", podcast.name, podcast.id)
            return GenerateBriefingResult(episode = null)
        }

        val generatingEpisode = episodeService.createGeneratingEpisode(podcast)

        return try {
            val result = llmPipeline.run(podcast) { stage, detail ->
                episodeService.updatePipelineStage(generatingEpisode.id!!, stage)
                eventPublisher.publishEvent(
                    PodcastEvent(this, podcast.id, "episode", generatingEpisode.id, "episode.stage",
                        detail + ("stage" to stage))
                )
            } ?: run {
                episodeRepository.delete(generatingEpisode)
                return GenerateBriefingResult(episode = null)
            }

            val episode = episodeService.createEpisodeFromPipelineResult(podcast, result, generatingEpisode)
            GenerateBriefingResult(episode = episode)
        } catch (e: Exception) {
            log.error("[Pipeline] Briefing generation failed for podcast '{}' ({}): {}", podcast.name, podcast.id, e.message, e)
            val failedEpisode = episodeService.failEpisode(podcast, e.message ?: "Unknown error", generatingEpisode)
            GenerateBriefingResult(episode = failedEpisode, failed = true, errorMessage = e.message)
        }
    }

    @Transactional
    fun regenerateEpisode(sourceEpisode: Episode, podcast: Podcast): Episode {
        val linkedArticles = episodeArticleRepository.findByEpisodeId(sourceEpisode.id!!)
        val articles = linkedArticles.mapNotNull { link ->
            articleRepository.findById(link.articleId).orElse(null)
        }
        if (articles.isEmpty()) {
            throw IllegalStateException("No articles found for episode ${sourceEpisode.id}")
        }

        val result = llmPipeline.recompose(articles, podcast) { stage, detail ->
            eventPublisher.publishEvent(
                PodcastEvent(this, podcast.id, "pipeline", 0, "pipeline.progress",
                    detail + ("stage" to stage))
            )
        }
        return episodeService.createEpisodeFromPipelineResult(
            podcast,
            result,
            overrideGeneratedAt = sourceEpisode.generatedAt,
            updateLastGenerated = false
        )
    }

    fun getUpcomingContent(podcast: Podcast): UpcomingContent {
        val sources = sourceRepository.findByPodcastId(podcast.id)
        val sourceIds = sources.map { it.id }
        if (sourceIds.isEmpty()) return UpcomingContent(emptyList(), emptyList(), sources, 0, 0)

        val since = podcast.lastGeneratedAt ?: Instant.now().minus(
            (podcast.maxArticleAgeDays ?: appProperties.source.maxArticleAgeDays).toLong(), ChronoUnit.DAYS
        ).toString()

        val articles = articleRepository.findUnprocessedBySourceIds(sourceIds)
        val unlinkedPosts = postRepository.findUnlinkedSince(sourceIds, since)

        val articleIds = articles.mapNotNull { it.id }
        val linkedPostCount = if (articleIds.isNotEmpty()) postArticleRepository.countByArticleIds(articleIds) else 0L
        val totalPostCount = linkedPostCount + unlinkedPosts.size

        val sourceMap = sources.associateBy { it.id }
        val unlinkedPostArticleCount = unlinkedPosts
            .groupBy { it.sourceId }
            .entries
            .sumOf { (sourceId, posts) ->
                val source = sourceMap[sourceId]
                if (source != null && sourceAggregator.shouldAggregate(source) && posts.size > 1) 1L else posts.size.toLong()
            }
        val effectiveArticleCount = articles.size.toLong() + unlinkedPostArticleCount

        return UpcomingContent(articles, unlinkedPosts, sources, totalPostCount, effectiveArticleCount)
    }

    @Transactional
    fun delete(podcastId: String): Boolean {
        val podcast = findById(podcastId) ?: return false
        deletePodcastCascade(podcast)
        return true
    }

    @Transactional
    fun deleteAllByUserId(userId: String) {
        val podcasts = podcastRepository.findByUserId(userId)
        for (podcast in podcasts) {
            deletePodcastCascade(podcast)
        }
    }

    private fun deletePodcastCascade(podcast: Podcast) {
        val sources = sourceRepository.findByPodcastId(podcast.id)
        for (source in sources) {
            articleRepository.deleteBySourceId(source.id)
            sourceRepository.delete(source)
        }

        val episodes = episodeRepository.findByPodcastId(podcast.id)
        for (episode in episodes) {
            episode.audioFilePath?.let { filePath ->
                try {
                    val audioPath = Path.of(filePath)
                    if (Files.exists(audioPath)) {
                        Files.delete(audioPath)
                    }
                } catch (e: Exception) {
                    log.error("Failed to delete audio file for episode {}: {}", episode.id, e.message)
                }
            }
            episodeRepository.delete(episode)
        }

        podcastRepository.delete(podcast)
        log.info("Deleted podcast {} and cascaded to {} sources, {} episodes", podcast.id, sources.size, episodes.size)
    }
}

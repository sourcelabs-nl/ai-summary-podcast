package com.aisummarypodcast.podcast

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.llm.LlmPipeline
import com.aisummarypodcast.llm.PreviewResult
import com.aisummarypodcast.store.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

data class UpcomingContent(
    val articles: List<Article>,
    val unlinkedPosts: List<Post>,
    val sources: List<Source>
)

@Service
class PodcastService(
    private val podcastRepository: PodcastRepository,
    private val sourceRepository: SourceRepository,
    private val articleRepository: ArticleRepository,
    private val postRepository: PostRepository,
    private val episodeRepository: EpisodeRepository,
    private val appProperties: AppProperties,
    private val llmPipeline: LlmPipeline,
    private val episodeService: EpisodeService
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
            pronunciations = podcast?.pronunciations
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
            pronunciations = updates.pronunciations
        )
        return podcastRepository.save(updated)
    }

    fun previewBriefing(podcast: Podcast, onProgress: (stage: String, detail: Map<String, Any>) -> Unit = { _, _ -> }): PreviewResult? {
        return llmPipeline.preview(podcast, onProgress)
    }

    fun generateBriefing(podcast: Podcast): Episode? {
        if (podcast.requireReview && episodeService.hasPendingOrApprovedEpisode(podcast.id)) {
            log.info("Podcast '{}' ({}) has a pending/approved episode — skipping generation", podcast.name, podcast.id)
            return null
        }

        val result = llmPipeline.run(podcast) ?: run {
            podcastRepository.save(podcast.copy(lastGeneratedAt = Instant.now().toString()))
            return null
        }

        return episodeService.createEpisodeFromPipelineResult(podcast, result)
    }

    fun getUpcomingContent(podcast: Podcast): UpcomingContent {
        val sources = sourceRepository.findByPodcastId(podcast.id)
        val sourceIds = sources.map { it.id }
        if (sourceIds.isEmpty()) return UpcomingContent(emptyList(), emptyList(), sources)

        val since = podcast.lastGeneratedAt ?: Instant.now().minus(
            (podcast.maxArticleAgeDays ?: appProperties.source.maxArticleAgeDays).toLong(), ChronoUnit.DAYS
        ).toString()

        val articles = articleRepository.findAllSince(sourceIds, since)
        val unlinkedPosts = postRepository.findUnlinkedSince(sourceIds, since)
        return UpcomingContent(articles, unlinkedPosts, sources)
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
            try {
                val audioPath = Path.of(episode.audioFilePath)
                if (Files.exists(audioPath)) {
                    Files.delete(audioPath)
                }
            } catch (e: Exception) {
                log.error("Failed to delete audio file for episode {}: {}", episode.id, e.message)
            }
            episodeRepository.delete(episode)
        }

        podcastRepository.delete(podcast)
        log.info("Deleted podcast {} and cascaded to {} sources, {} episodes", podcast.id, sources.size, episodes.size)
    }
}

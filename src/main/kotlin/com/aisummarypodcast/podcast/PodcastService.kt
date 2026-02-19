package com.aisummarypodcast.podcast

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.store.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

@Service
class PodcastService(
    private val podcastRepository: PodcastRepository,
    private val sourceRepository: SourceRepository,
    private val articleRepository: ArticleRepository,
    private val episodeRepository: EpisodeRepository,
    private val appProperties: AppProperties
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
            ttsProvider = podcast?.ttsProvider ?: "openai",
            ttsVoices = podcast?.ttsVoices,
            ttsSettings = podcast?.ttsSettings,
            style = podcast?.style ?: "news-briefing",
            targetWords = podcast?.targetWords,
            cron = podcast?.cron ?: "0 0 6 * * *",
            customInstructions = podcast?.customInstructions,
            relevanceThreshold = podcast?.relevanceThreshold ?: 5,
            requireReview = podcast?.requireReview ?: false,
            maxLlmCostCents = podcast?.maxLlmCostCents,
            maxArticleAgeDays = podcast?.maxArticleAgeDays,
            speakerNames = podcast?.speakerNames
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
            speakerNames = updates.speakerNames
        )
        return podcastRepository.save(updated)
    }

    fun delete(podcastId: String): Boolean {
        val podcast = findById(podcastId) ?: return false
        deletePodcastCascade(podcast)
        return true
    }

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

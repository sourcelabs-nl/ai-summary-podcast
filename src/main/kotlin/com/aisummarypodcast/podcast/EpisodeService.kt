package com.aisummarypodcast.podcast

import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.store.PodcastRepository
import com.aisummarypodcast.tts.TtsPipeline
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class EpisodeService(
    private val episodeRepository: EpisodeRepository,
    private val podcastRepository: PodcastRepository,
    private val ttsPipeline: TtsPipeline
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    fun generateAudioAsync(episodeId: Long, podcastId: String) {
        val episode = episodeRepository.findById(episodeId).orElse(null)
        if (episode == null) {
            log.error("Episode {} not found for async TTS generation", episodeId)
            return
        }

        val podcast = podcastRepository.findById(podcastId).orElse(null)
        if (podcast == null) {
            log.error("Podcast {} not found for async TTS generation", podcastId)
            episodeRepository.save(episode.copy(status = "FAILED"))
            return
        }

        try {
            log.info("Starting async TTS generation for episode {} (podcast {})", episodeId, podcastId)
            val generatedEpisode = ttsPipeline.generateForExistingEpisode(episode, podcast)
            log.info("Async TTS generation complete for episode {} (podcast {})", episodeId, podcastId)
        } catch (e: Exception) {
            log.error("Async TTS generation failed for episode {}: {}", episodeId, e.message, e)
            episodeRepository.save(episode.copy(status = "FAILED"))
        }
    }
}

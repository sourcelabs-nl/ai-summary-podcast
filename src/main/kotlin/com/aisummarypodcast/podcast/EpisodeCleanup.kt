package com.aisummarypodcast.podcast

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.store.EpisodePublicationRepository
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.store.PodcastRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class EpisodeCleanup(
    private val episodeRepository: EpisodeRepository,
    private val podcastRepository: PodcastRepository,
    private val appProperties: AppProperties,
    private val staticFeedExporter: StaticFeedExporter,
    private val publicationRepository: EpisodePublicationRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 3 * * *")
    fun cleanup() {
        val cutoff = Instant.now().minus(appProperties.episodes.retentionDays.toLong(), ChronoUnit.DAYS).toString()

        for (podcast in podcastRepository.findAll()) {
            val oldEpisodes = episodeRepository.findByPodcastId(podcast.id)
                .filter { it.generatedAt < cutoff }

            if (oldEpisodes.isEmpty()) continue

            for (episode in oldEpisodes) {
                val audioPath = Path.of(episode.audioFilePath)
                try {
                    if (Files.exists(audioPath)) {
                        Files.delete(audioPath)
                    } else {
                        log.warn("MP3 file not found for episode {}: {}", episode.id, audioPath)
                    }
                } catch (e: Exception) {
                    log.error("Failed to delete MP3 file for episode {}: {}", episode.id, e.message)
                }
                publicationRepository.deleteByEpisodeId(episode.id!!)
                episodeRepository.delete(episode)
            }

            log.info("Cleaned up {} old episodes for podcast {}", oldEpisodes.size, podcast.id)
            staticFeedExporter.export(podcast)
        }
    }
}

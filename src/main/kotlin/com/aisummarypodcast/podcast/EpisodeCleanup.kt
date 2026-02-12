package com.aisummarypodcast.podcast

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.store.EpisodeRepository
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
    private val appProperties: AppProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 3 * * *") // Run daily at 3 AM
    fun cleanup() {
        val cutoff = Instant.now().minus(appProperties.episodes.retentionDays.toLong(), ChronoUnit.DAYS).toString()
        val oldEpisodes = episodeRepository.findOlderThan(cutoff)

        if (oldEpisodes.isEmpty()) {
            log.info("No episodes older than {} days to clean up", appProperties.episodes.retentionDays)
            return
        }

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
            episodeRepository.delete(episode)
        }

        log.info("Cleaned up {} old episodes", oldEpisodes.size)
    }
}

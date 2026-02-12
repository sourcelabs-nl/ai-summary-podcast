package com.aisummarypodcast.scheduler

import com.aisummarypodcast.llm.LlmPipeline
import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.store.PodcastRepository
import com.aisummarypodcast.tts.TtsPipeline
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.support.CronExpression
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@Component
class BriefingGenerationScheduler(
    private val podcastRepository: PodcastRepository,
    private val llmPipeline: LlmPipeline,
    private val ttsPipeline: TtsPipeline,
    private val episodeRepository: EpisodeRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 60_000)
    fun checkAndGenerate() {
        val podcasts = podcastRepository.findAll()
        val now = LocalDateTime.now(ZoneOffset.UTC)

        for (podcast in podcasts) {
            try {
                val cronExpression = CronExpression.parse(podcast.cron)
                val lastGenerated = podcast.lastGeneratedAt?.let {
                    LocalDateTime.ofInstant(Instant.parse(it), ZoneOffset.UTC)
                }

                val nextExecution = if (lastGenerated != null) {
                    cronExpression.next(lastGenerated)
                } else {
                    // Never generated — check if it should have run today
                    val startOfDay = now.toLocalDate().atStartOfDay()
                    cronExpression.next(startOfDay)
                }

                if (nextExecution != null && !nextExecution.isAfter(now)) {
                    log.info("Podcast {} is due for briefing generation", podcast.id)
                    generateBriefing(podcast.id)
                }
            } catch (e: Exception) {
                log.error("Error checking/generating briefing for podcast {}: {}", podcast.id, e.message, e)
            }
        }
    }

    private fun generateBriefing(podcastId: String) {
        val podcast = podcastRepository.findById(podcastId).orElse(null) ?: return

        if (podcast.requireReview && hasPendingOrApprovedEpisode(podcastId)) {
            log.info("Podcast {} has a pending/approved episode — skipping generation", podcastId)
            return
        }

        val script = llmPipeline.run(podcast)
        if (script == null) {
            log.info("No briefing script generated for podcast {} — nothing to synthesize", podcastId)
            podcastRepository.save(podcast.copy(lastGeneratedAt = Instant.now().toString()))
            return
        }

        if (podcast.requireReview) {
            episodeRepository.save(
                Episode(
                    podcastId = podcastId,
                    generatedAt = Instant.now().toString(),
                    scriptText = script,
                    status = "PENDING_REVIEW"
                )
            )
            podcastRepository.save(podcast.copy(lastGeneratedAt = Instant.now().toString()))
            log.info("Script ready for review for podcast {}", podcastId)
        } else {
            val episode = ttsPipeline.generate(script, podcast)
            podcastRepository.save(podcast.copy(lastGeneratedAt = Instant.now().toString()))
            log.info("Briefing generation complete for podcast {}: episode {} ({} seconds)", podcastId, episode.id, episode.durationSeconds)
        }
    }

    private fun hasPendingOrApprovedEpisode(podcastId: String): Boolean {
        return episodeRepository.findByPodcastIdAndStatusIn(
            podcastId, listOf("PENDING_REVIEW", "APPROVED")
        ).isNotEmpty()
    }
}

package com.aisummarypodcast.scheduler

import com.aisummarypodcast.podcast.PodcastService
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PodcastRepository
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.support.CronExpression
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.time.TimeSource

@Component
class BriefingGenerationScheduler(
    private val podcastRepository: PodcastRepository,
    private val podcastService: PodcastService,
    private val clock: Clock = Clock.systemUTC()
) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        val STALENESS_WINDOW: Duration = Duration.ofMinutes(30)
    }

    @EventListener(ApplicationReadyEvent::class)
    fun start() {
        scope.launch {
            while (isActive) {
                try {
                    checkAndGenerate()
                } catch (e: Exception) {
                    log.error("[Pipeline] Unexpected error in briefing generation loop", e)
                }
                delay(60_000)
            }
        }
    }

    @PreDestroy
    fun stop() {
        scope.cancel()
    }

    fun checkAndGenerate() {
        val podcasts = podcastRepository.findAll()

        for (podcast in podcasts) {
            try {
                val zone = try {
                    ZoneId.of(podcast.timezone)
                } catch (_: Exception) {
                    log.warn("[Pipeline] Invalid timezone '{}' for podcast '{}' ({}), falling back to UTC", podcast.timezone, podcast.name, podcast.id)
                    ZoneOffset.UTC
                }
                val now = LocalDateTime.now(clock.withZone(zone))
                val cronExpression = CronExpression.parse(podcast.cron)
                val lastGenerated = podcast.lastGeneratedAt?.let {
                    LocalDateTime.ofInstant(Instant.parse(it), zone)
                }

                var nextExecution = if (lastGenerated != null) {
                    cronExpression.next(lastGenerated)
                } else {
                    // Never generated — check if it should have run today
                    val startOfDay = now.toLocalDate().atStartOfDay()
                    cronExpression.next(startOfDay)
                }

                // Skip stale triggers that are beyond the staleness window
                while (nextExecution != null && !nextExecution.isAfter(now)
                    && Duration.between(nextExecution, now) > STALENESS_WINDOW
                ) {
                    log.warn("[Pipeline] Skipping stale trigger at {} for podcast '{}' ({})", nextExecution, podcast.name, podcast.id)
                    nextExecution = cronExpression.next(nextExecution)
                }

                if (nextExecution != null && !nextExecution.isAfter(now)) {
                    log.info("[Pipeline] Podcast '{}' ({}) is due for briefing generation", podcast.name, podcast.id)
                    generateBriefing(podcast)
                }
            } catch (e: Exception) {
                log.error("[Pipeline] Error checking/generating briefing for podcast '{}' ({}): {}", podcast.name, podcast.id, e.message, e)
            }
        }
    }

    private fun generateBriefing(podcast: Podcast) {
        log.info("[Pipeline] Starting briefing generation for podcast '{}' ({})", podcast.name, podcast.id)
        val mark = TimeSource.Monotonic.markNow()

        val result = podcastService.generateBriefing(podcast)
        if (result.failed) {
            log.error("[Pipeline] Briefing generation failed for podcast '{}' ({}): {} — total {}", podcast.name, podcast.id, result.errorMessage, mark.elapsedNow())
            return
        }
        if (result.episode == null) {
            log.info("[Pipeline] No briefing generated for podcast '{}' ({}) — skipped or no articles ({})", podcast.name, podcast.id, mark.elapsedNow())
            return
        }

        log.info("[Pipeline] Briefing generation complete for podcast '{}' ({}): episode {} — total {}", podcast.name, podcast.id, result.episode.id, mark.elapsedNow())
    }
}

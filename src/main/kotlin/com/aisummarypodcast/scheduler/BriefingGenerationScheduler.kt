package com.aisummarypodcast.scheduler

import com.aisummarypodcast.llm.LlmPipeline
import com.aisummarypodcast.llm.PipelineResult
import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.EpisodeArticle
import com.aisummarypodcast.store.EpisodeArticleRepository
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
import kotlin.time.TimeSource

@Component
class BriefingGenerationScheduler(
    private val podcastRepository: PodcastRepository,
    private val llmPipeline: LlmPipeline,
    private val ttsPipeline: TtsPipeline,
    private val episodeRepository: EpisodeRepository,
    private val episodeArticleRepository: EpisodeArticleRepository
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
                    log.info("[Pipeline] Podcast {} is due for briefing generation", podcast.id)
                    generateBriefing(podcast.id)
                }
            } catch (e: Exception) {
                log.error("[Pipeline] Error checking/generating briefing for podcast {}: {}", podcast.id, e.message, e)
            }
        }
    }

    private fun generateBriefing(podcastId: String) {
        log.info("[Pipeline] Starting briefing generation for podcast {}", podcastId)
        val mark = TimeSource.Monotonic.markNow()

        val podcast = podcastRepository.findById(podcastId).orElse(null) ?: return

        if (podcast.requireReview && hasPendingOrApprovedEpisode(podcastId)) {
            log.info("[Pipeline] Podcast {} has a pending/approved episode — skipping generation ({})", podcastId, mark.elapsedNow())
            return
        }

        val result = llmPipeline.run(podcast)
        if (result == null) {
            log.info("[Pipeline] No briefing script generated for podcast {} — nothing to synthesize ({})", podcastId, mark.elapsedNow())
            podcastRepository.save(podcast.copy(lastGeneratedAt = Instant.now().toString()))
            return
        }

        if (podcast.requireReview) {
            val savedEpisode = episodeRepository.save(
                Episode(
                    podcastId = podcastId,
                    generatedAt = Instant.now().toString(),
                    scriptText = result.script,
                    status = "PENDING_REVIEW",
                    filterModel = result.filterModel,
                    composeModel = result.composeModel,
                    llmInputTokens = result.llmInputTokens,
                    llmOutputTokens = result.llmOutputTokens,
                    llmCostCents = result.llmCostCents
                )
            )
            saveEpisodeArticleLinks(savedEpisode, result)
            podcastRepository.save(podcast.copy(lastGeneratedAt = Instant.now().toString()))
            log.info("[Pipeline] Script ready for review for podcast {} — total {}", podcastId, mark.elapsedNow())
        } else {
            val episode = ttsPipeline.generate(result.script, podcast)
            val episodeWithModels = episode.copy(
                filterModel = result.filterModel,
                composeModel = result.composeModel,
                llmInputTokens = result.llmInputTokens,
                llmOutputTokens = result.llmOutputTokens,
                llmCostCents = result.llmCostCents
            )
            val savedEpisode = episodeRepository.save(episodeWithModels)
            saveEpisodeArticleLinks(savedEpisode, result)
            podcastRepository.save(podcast.copy(lastGeneratedAt = Instant.now().toString()))
            log.info("[Pipeline] Briefing generation complete for podcast {}: episode {} ({} seconds) — total {}", podcastId, savedEpisode.id, savedEpisode.durationSeconds, mark.elapsedNow())
        }
    }

    private fun saveEpisodeArticleLinks(episode: Episode, result: PipelineResult) {
        for (articleId in result.processedArticleIds) {
            episodeArticleRepository.save(EpisodeArticle(episodeId = episode.id!!, articleId = articleId))
        }
    }

    private fun hasPendingOrApprovedEpisode(podcastId: String): Boolean {
        return episodeRepository.findByPodcastIdAndStatusIn(
            podcastId, listOf("PENDING_REVIEW", "APPROVED")
        ).isNotEmpty()
    }
}

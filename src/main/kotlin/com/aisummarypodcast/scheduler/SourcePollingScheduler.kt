package com.aisummarypodcast.scheduler

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.podcast.PodcastService
import com.aisummarypodcast.source.SourcePoller
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.PostRepository
import com.aisummarypodcast.store.SourceRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import com.aisummarypodcast.store.Source
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.min

@Component
class SourcePollingScheduler(
    private val sourcePoller: SourcePoller,
    private val sourceRepository: SourceRepository,
    private val articleRepository: ArticleRepository,
    private val postRepository: PostRepository,
    private val appProperties: AppProperties,
    private val podcastService: PodcastService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 60_000)
    fun pollSources() {
        cleanupOldArticles()

        val allSources = sourceRepository.findAll().filter { it.enabled }
        log.info("[Polling] Checking {} enabled sources", allSources.count())

        for (source in allSources) {
            val lastPolled = source.lastPolled?.let { Instant.parse(it) }
            val effectiveInterval = effectivePollIntervalMinutes(source)

            if (lastPolled == null || lastPolled.plus(effectiveInterval, ChronoUnit.MINUTES).isBefore(Instant.now())) {
                val podcast = podcastService.findById(source.podcastId)
                val userId = if (source.type == "twitter") podcast?.userId else null
                val maxArticleAgeDays = podcast?.maxArticleAgeDays ?: appProperties.source.maxArticleAgeDays
                sourcePoller.poll(source, userId, maxArticleAgeDays)
            }
        }
    }

    internal fun effectivePollIntervalMinutes(source: Source): Long {
        if (source.consecutiveFailures == 0) return source.pollIntervalMinutes.toLong()
        val maxBackoffMinutes = (source.maxBackoffHours ?: appProperties.source.maxBackoffHours).toLong() * 60
        val backoff = source.pollIntervalMinutes.toLong() * (1L shl min(source.consecutiveFailures, 30))
        return min(backoff, maxBackoffMinutes)
    }

    private fun cleanupOldArticles() {
        val cutoff = Instant.now().minus(appProperties.source.maxArticleAgeDays.toLong(), ChronoUnit.DAYS)
        articleRepository.deleteOldUnprocessedArticles(cutoff.toString())
        postRepository.deleteOldUnlinkedPosts(cutoff.toString())
    }
}

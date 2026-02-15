package com.aisummarypodcast.scheduler

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.podcast.PodcastService
import com.aisummarypodcast.source.SourcePoller
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.SourceRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class SourcePollingScheduler(
    private val sourcePoller: SourcePoller,
    private val sourceRepository: SourceRepository,
    private val articleRepository: ArticleRepository,
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

            if (lastPolled == null || lastPolled.plus(source.pollIntervalMinutes.toLong(), ChronoUnit.MINUTES).isBefore(Instant.now())) {
                val userId = if (source.type == "twitter") {
                    podcastService.findById(source.podcastId)?.userId
                } else {
                    null
                }
                sourcePoller.poll(source, userId)
            }
        }
    }

    private fun cleanupOldArticles() {
        val cutoff = Instant.now().minus(appProperties.source.maxArticleAgeDays.toLong(), ChronoUnit.DAYS)
        articleRepository.deleteOldUnprocessedArticles(cutoff.toString())
    }
}

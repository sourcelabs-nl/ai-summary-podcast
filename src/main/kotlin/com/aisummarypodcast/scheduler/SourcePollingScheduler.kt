package com.aisummarypodcast.scheduler

import com.aisummarypodcast.source.SourcePoller
import com.aisummarypodcast.store.SourceRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class SourcePollingScheduler(
    private val sourcePoller: SourcePoller,
    private val sourceRepository: SourceRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 60_000)
    fun pollSources() {
        val allSources = sourceRepository.findAll().filter { it.enabled }
        log.debug("Checking {} enabled sources for polling", allSources.count())

        for (source in allSources) {
            val lastPolled = source.lastPolled?.let { Instant.parse(it) }

            if (lastPolled == null || lastPolled.plus(source.pollIntervalMinutes.toLong(), ChronoUnit.MINUTES).isBefore(Instant.now())) {
                sourcePoller.poll(source)
            }
        }
    }
}

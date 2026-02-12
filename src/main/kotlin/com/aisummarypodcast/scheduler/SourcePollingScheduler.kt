package com.aisummarypodcast.scheduler

import com.aisummarypodcast.config.SourceProperties
import com.aisummarypodcast.source.SourcePoller
import com.aisummarypodcast.store.SourceRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class SourcePollingScheduler(
    private val sourceProperties: SourceProperties,
    private val sourcePoller: SourcePoller,
    private val sourceRepository: SourceRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 60_000)
    fun pollSources() {
        val enabledSources = sourceProperties.entries.filter { it.enabled }
        log.debug("Checking {} enabled sources for polling", enabledSources.size)

        for (sourceEntry in enabledSources) {
            val source = sourceRepository.findById(sourceEntry.id).orElse(null)
            val lastPolled = source?.lastPolled?.let { Instant.parse(it) }

            if (lastPolled == null || lastPolled.plus(sourceEntry.pollIntervalMinutes.toLong(), ChronoUnit.MINUTES).isBefore(Instant.now())) {
                sourcePoller.poll(sourceEntry)
            }
        }
    }
}

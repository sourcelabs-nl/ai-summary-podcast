package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.store.LlmCacheRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class LlmCacheCleanup(
    private val llmCacheRepository: LlmCacheRepository,
    private val appProperties: AppProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "0 0 3 * * *")
    fun cleanup() {
        val maxAgeDays = appProperties.llmCache.maxAgeDays ?: return

        val cutoff = Instant.now().minus(maxAgeDays.toLong(), ChronoUnit.DAYS).toString()
        llmCacheRepository.deleteOlderThan(cutoff)
        log.info("LLM cache cleanup: deleted entries older than {} days (cutoff: {})", maxAgeDays, cutoff)
    }
}

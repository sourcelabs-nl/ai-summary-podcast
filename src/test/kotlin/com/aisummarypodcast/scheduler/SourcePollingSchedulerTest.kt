package com.aisummarypodcast.scheduler

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.BriefingProperties
import com.aisummarypodcast.config.EncryptionProperties
import com.aisummarypodcast.config.EpisodesProperties
import com.aisummarypodcast.config.FeedProperties
import com.aisummarypodcast.config.LlmProperties
import com.aisummarypodcast.config.SourceProperties
import com.aisummarypodcast.source.SourcePoller
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.SourceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class SourcePollingSchedulerTest {

    private val sourcePoller = mockk<SourcePoller>()
    private val sourceRepository = mockk<SourceRepository> {
        every { findAll() } returns emptyList()
    }
    private val articleRepository = mockk<ArticleRepository> {
        every { deleteOldUnprocessedArticles(any()) } returns Unit
    }

    private fun appProperties(maxArticleAgeDays: Int = 7) = AppProperties(
        llm = LlmProperties(),
        briefing = BriefingProperties(),
        episodes = EpisodesProperties(),
        feed = FeedProperties(),
        encryption = EncryptionProperties(masterKey = "test-key"),
        source = SourceProperties(maxArticleAgeDays = maxArticleAgeDays)
    )

    @Test
    fun `cleans up old unprocessed articles before polling`() {
        val scheduler = SourcePollingScheduler(sourcePoller, sourceRepository, articleRepository, appProperties())

        scheduler.pollSources()

        verify { articleRepository.deleteOldUnprocessedArticles(match { cutoff ->
            val parsed = Instant.parse(cutoff)
            val expectedCutoff = Instant.now().minus(7, ChronoUnit.DAYS)
            // Allow 5 seconds of tolerance for test execution time
            parsed.isAfter(expectedCutoff.minusSeconds(5)) && parsed.isBefore(expectedCutoff.plusSeconds(5))
        }) }
    }
}

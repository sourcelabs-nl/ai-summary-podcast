package com.aisummarypodcast.scheduler

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.BriefingProperties
import com.aisummarypodcast.config.EncryptionProperties
import com.aisummarypodcast.config.EpisodesProperties
import com.aisummarypodcast.config.FeedProperties
import com.aisummarypodcast.config.LlmProperties
import com.aisummarypodcast.config.SourceProperties
import com.aisummarypodcast.podcast.PodcastService
import com.aisummarypodcast.source.SourcePoller
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.PostRepository
import com.aisummarypodcast.store.Source
import com.aisummarypodcast.store.SourceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class SourcePollingSchedulerBackoffTest {

    private val sourcePoller = mockk<SourcePoller>()
    private val sourceRepository = mockk<SourceRepository>()
    private val articleRepository = mockk<ArticleRepository> {
        every { deleteOldUnprocessedArticles(any()) } returns Unit
    }
    private val postRepository = mockk<PostRepository> {
        every { deleteOldUnlinkedPosts(any()) } returns Unit
    }
    private val podcastService = mockk<PodcastService>()

    private fun appProperties(maxBackoffHours: Int = 24) = AppProperties(
        llm = LlmProperties(),
        briefing = BriefingProperties(),
        episodes = EpisodesProperties(),
        feed = FeedProperties(),
        encryption = EncryptionProperties(masterKey = "test-key"),
        source = SourceProperties(maxBackoffHours = maxBackoffHours)
    )

    private fun scheduler(maxBackoffHours: Int = 24) = SourcePollingScheduler(
        sourcePoller, sourceRepository, articleRepository, postRepository, appProperties(maxBackoffHours), podcastService
    )

    @Test
    fun `effectivePollIntervalMinutes returns normal interval when no failures`() {
        val source = Source(id = "s1", podcastId = "p1", type = "rss", url = "u", pollIntervalMinutes = 60, consecutiveFailures = 0)
        assertEquals(60L, scheduler().effectivePollIntervalMinutes(source))
    }

    @Test
    fun `effectivePollIntervalMinutes doubles on first failure`() {
        val source = Source(id = "s1", podcastId = "p1", type = "rss", url = "u", pollIntervalMinutes = 60, consecutiveFailures = 1)
        assertEquals(120L, scheduler().effectivePollIntervalMinutes(source))
    }

    @Test
    fun `effectivePollIntervalMinutes is 8x on third failure`() {
        val source = Source(id = "s1", podcastId = "p1", type = "rss", url = "u", pollIntervalMinutes = 60, consecutiveFailures = 3)
        assertEquals(480L, scheduler().effectivePollIntervalMinutes(source))
    }

    @Test
    fun `effectivePollIntervalMinutes is capped at maxBackoffHours`() {
        val source = Source(id = "s1", podcastId = "p1", type = "rss", url = "u", pollIntervalMinutes = 60, consecutiveFailures = 10)
        assertEquals(1440L, scheduler(maxBackoffHours = 24).effectivePollIntervalMinutes(source))
    }

    @Test
    fun `effectivePollIntervalMinutes respects custom maxBackoffHours`() {
        val source = Source(id = "s1", podcastId = "p1", type = "rss", url = "u", pollIntervalMinutes = 60, consecutiveFailures = 10)
        assertEquals(360L, scheduler(maxBackoffHours = 6).effectivePollIntervalMinutes(source))
    }

    @Test
    fun `source with failures is skipped when backoff interval has not elapsed`() {
        val source = Source(
            id = "s1", podcastId = "p1", type = "rss", url = "u",
            pollIntervalMinutes = 60, consecutiveFailures = 2,
            lastPolled = Instant.now().minus(100, ChronoUnit.MINUTES).toString(), // 100 min ago, but backoff is 240 min
            enabled = true
        )
        every { sourceRepository.findAll() } returns listOf(source)

        scheduler().pollSources()

        verify(exactly = 0) { sourcePoller.poll(any(), any()) }
    }

    @Test
    fun `source with failures is polled when backoff interval has elapsed`() {
        val source = Source(
            id = "s1", podcastId = "p1", type = "rss", url = "u",
            pollIntervalMinutes = 60, consecutiveFailures = 1,
            lastPolled = Instant.now().minus(130, ChronoUnit.MINUTES).toString(), // 130 min ago, backoff is 120 min
            enabled = true
        )
        every { sourceRepository.findAll() } returns listOf(source)
        every { sourcePoller.poll(source, null) } returns Unit

        scheduler().pollSources()

        verify { sourcePoller.poll(source, null) }
    }
}

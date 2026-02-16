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
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PostRepository
import com.aisummarypodcast.store.Source
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
    private val postRepository = mockk<PostRepository> {
        every { deleteOldUnlinkedPosts(any()) } returns Unit
    }
    private val podcastService = mockk<PodcastService>()

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
        val scheduler = SourcePollingScheduler(sourcePoller, sourceRepository, articleRepository, postRepository, appProperties(), podcastService)

        scheduler.pollSources()

        verify { articleRepository.deleteOldUnprocessedArticles(match { cutoff ->
            val parsed = Instant.parse(cutoff)
            val expectedCutoff = Instant.now().minus(7, ChronoUnit.DAYS)
            // Allow 5 seconds of tolerance for test execution time
            parsed.isAfter(expectedCutoff.minusSeconds(5)) && parsed.isBefore(expectedCutoff.plusSeconds(5))
        }) }
    }

    @Test
    fun `cleans up old unlinked posts before polling`() {
        val scheduler = SourcePollingScheduler(sourcePoller, sourceRepository, articleRepository, postRepository, appProperties(), podcastService)

        scheduler.pollSources()

        verify { postRepository.deleteOldUnlinkedPosts(match { cutoff ->
            val parsed = Instant.parse(cutoff)
            val expectedCutoff = Instant.now().minus(7, ChronoUnit.DAYS)
            parsed.isAfter(expectedCutoff.minusSeconds(5)) && parsed.isBefore(expectedCutoff.plusSeconds(5))
        }) }
    }

    @Test
    fun `resolves podcast owner userId for twitter source`() {
        val twitterSource = Source(
            id = "s1", podcastId = "p1", type = "twitter", url = "testuser",
            lastPolled = null, enabled = true
        )
        val podcast = Podcast(
            id = "p1", userId = "owner-1", name = "Test", topic = "tech"
        )

        every { sourceRepository.findAll() } returns listOf(twitterSource)
        every { podcastService.findById("p1") } returns podcast
        every { sourcePoller.poll(twitterSource, "owner-1", 7) } returns Unit

        val scheduler = SourcePollingScheduler(sourcePoller, sourceRepository, articleRepository, postRepository, appProperties(), podcastService)
        scheduler.pollSources()

        verify { podcastService.findById("p1") }
        verify { sourcePoller.poll(twitterSource, "owner-1", 7) }
    }

    @Test
    fun `does not resolve userId for rss source`() {
        val rssSource = Source(
            id = "s2", podcastId = "p1", type = "rss", url = "https://example.com/feed",
            lastPolled = null, enabled = true
        )

        val podcast = Podcast(id = "p1", userId = "owner-1", name = "Test", topic = "tech")
        every { sourceRepository.findAll() } returns listOf(rssSource)
        every { podcastService.findById("p1") } returns podcast
        every { sourcePoller.poll(rssSource, null, 7) } returns Unit

        val scheduler = SourcePollingScheduler(sourcePoller, sourceRepository, articleRepository, postRepository, appProperties(), podcastService)
        scheduler.pollSources()

        verify { sourcePoller.poll(rssSource, null, 7) }
    }
}

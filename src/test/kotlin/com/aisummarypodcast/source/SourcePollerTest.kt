package com.aisummarypodcast.source

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.BriefingProperties
import com.aisummarypodcast.config.EncryptionProperties
import com.aisummarypodcast.config.EpisodesProperties
import com.aisummarypodcast.config.FeedProperties
import com.aisummarypodcast.config.LlmProperties
import com.aisummarypodcast.config.SourceProperties
import com.aisummarypodcast.store.Post
import com.aisummarypodcast.store.PostRepository
import com.aisummarypodcast.store.Source
import com.aisummarypodcast.store.SourceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class SourcePollerTest {

    private val rssFeedFetcher = mockk<RssFeedFetcher>()
    private val websiteFetcher = mockk<WebsiteFetcher>()
    private val twitterFetcher = mockk<TwitterFetcher>()
    private val postRepository = mockk<PostRepository> {
        every { findBySourceIdAndContentHash(any(), any()) } returns null
        every { save(any()) } answers { firstArg() }
    }
    private val sourceRepository = mockk<SourceRepository> {
        every { save(any()) } answers { firstArg() }
    }

    private fun appProperties(maxArticleAgeDays: Int = 7) = AppProperties(
        llm = LlmProperties(),
        briefing = BriefingProperties(),
        episodes = EpisodesProperties(),
        feed = FeedProperties(),
        encryption = EncryptionProperties(masterKey = "test-key"),
        source = SourceProperties(maxArticleAgeDays = maxArticleAgeDays)
    )

    private val source = Source(id = "s1", podcastId = "p1", type = "rss", url = "https://example.com/feed")

    private fun post(publishedAt: String? = null) = Post(
        sourceId = "s1",
        title = "Test Post",
        body = "Post body content",
        url = "https://example.com/post",
        publishedAt = publishedAt,
        contentHash = "",
        createdAt = ""
    )

    @Test
    fun `saves post within max age`() {
        val recentPost = post(publishedAt = Instant.now().minus(2, ChronoUnit.DAYS).toString())
        every { rssFeedFetcher.fetch(any(), any(), any()) } returns listOf(recentPost)

        val poller = SourcePoller(rssFeedFetcher, websiteFetcher, twitterFetcher, postRepository, sourceRepository, appProperties())
        poller.poll(source)

        verify { postRepository.save(any()) }
    }

    @Test
    fun `skips post older than max age`() {
        val oldPost = post(publishedAt = Instant.now().minus(30, ChronoUnit.DAYS).toString())
        every { rssFeedFetcher.fetch(any(), any(), any()) } returns listOf(oldPost)

        val poller = SourcePoller(rssFeedFetcher, websiteFetcher, twitterFetcher, postRepository, sourceRepository, appProperties())
        poller.poll(source)

        verify(exactly = 0) { postRepository.save(match { it.sourceId == "s1" && it.title == "Test Post" }) }
    }

    @Test
    fun `saves post with null publishedAt`() {
        val nullDatePost = post(publishedAt = null)
        every { rssFeedFetcher.fetch(any(), any(), any()) } returns listOf(nullDatePost)

        val poller = SourcePoller(rssFeedFetcher, websiteFetcher, twitterFetcher, postRepository, sourceRepository, appProperties())
        poller.poll(source)

        verify { postRepository.save(any()) }
    }

    @Test
    fun `respects custom max article age`() {
        val post10DaysOld = post(publishedAt = Instant.now().minus(10, ChronoUnit.DAYS).toString())
        every { rssFeedFetcher.fetch(any(), any(), any()) } returns listOf(post10DaysOld)

        val pollerWith7Days = SourcePoller(rssFeedFetcher, websiteFetcher, twitterFetcher, postRepository, sourceRepository, appProperties(maxArticleAgeDays = 7))
        pollerWith7Days.poll(source)
        verify(exactly = 0) { postRepository.save(match { it.title == "Test Post" }) }

        val pollerWith14Days = SourcePoller(rssFeedFetcher, websiteFetcher, twitterFetcher, postRepository, sourceRepository, appProperties(maxArticleAgeDays = 14))
        pollerWith14Days.poll(source)
        verify { postRepository.save(any()) }
    }

    @Test
    fun `dispatches twitter source to twitterFetcher with userId`() {
        val twitterSource = Source(id = "s2", podcastId = "p1", type = "twitter", url = "testuser")
        val tweetPost = Post(
            sourceId = "s2",
            title = "Tweet text",
            body = "Tweet text",
            url = "https://x.com/testuser/status/123",
            publishedAt = Instant.now().toString(),
            author = "@testuser",
            contentHash = "",
            createdAt = ""
        )
        every { twitterFetcher.fetch("testuser", "s2", null, "user1") } returns listOf(tweetPost)
        every { twitterFetcher.buildLastSeenId(null, any(), "testuser", "user1") } returns "999:123"

        val poller = SourcePoller(rssFeedFetcher, websiteFetcher, twitterFetcher, postRepository, sourceRepository, appProperties())
        poller.poll(twitterSource, userId = "user1")

        verify { postRepository.save(any()) }
        verify { twitterFetcher.fetch("testuser", "s2", null, "user1") }
    }

    @Test
    fun `skips twitter source when no userId provided`() {
        val twitterSource = Source(id = "s2", podcastId = "p1", type = "twitter", url = "testuser")

        val poller = SourcePoller(rssFeedFetcher, websiteFetcher, twitterFetcher, postRepository, sourceRepository, appProperties())
        poller.poll(twitterSource, userId = null)

        verify(exactly = 0) { twitterFetcher.fetch(any(), any(), any(), any()) }
    }
}

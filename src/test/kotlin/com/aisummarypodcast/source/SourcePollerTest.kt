package com.aisummarypodcast.source

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.BriefingProperties
import com.aisummarypodcast.config.EncryptionProperties
import com.aisummarypodcast.config.EpisodesProperties
import com.aisummarypodcast.config.FeedProperties
import com.aisummarypodcast.config.LlmProperties
import com.aisummarypodcast.config.SourceProperties
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.ArticleRepository
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
    private val articleRepository = mockk<ArticleRepository> {
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

    private fun article(publishedAt: String? = null) = Article(
        sourceId = "s1",
        title = "Test Article",
        body = "Article body content",
        url = "https://example.com/article",
        publishedAt = publishedAt,
        contentHash = ""
    )

    @Test
    fun `saves article within max age`() {
        val recentArticle = article(publishedAt = Instant.now().minus(2, ChronoUnit.DAYS).toString())
        every { rssFeedFetcher.fetch(any(), any(), any()) } returns listOf(recentArticle)

        val poller = SourcePoller(rssFeedFetcher, websiteFetcher, twitterFetcher, articleRepository, sourceRepository, appProperties())
        poller.poll(source)

        verify { articleRepository.save(any()) }
    }

    @Test
    fun `skips article older than max age`() {
        val oldArticle = article(publishedAt = Instant.now().minus(30, ChronoUnit.DAYS).toString())
        every { rssFeedFetcher.fetch(any(), any(), any()) } returns listOf(oldArticle)

        val poller = SourcePoller(rssFeedFetcher, websiteFetcher, twitterFetcher, articleRepository, sourceRepository, appProperties())
        poller.poll(source)

        verify(exactly = 0) { articleRepository.save(match { it.sourceId == "s1" && it.title == "Test Article" }) }
    }

    @Test
    fun `saves article with null publishedAt`() {
        val nullDateArticle = article(publishedAt = null)
        every { rssFeedFetcher.fetch(any(), any(), any()) } returns listOf(nullDateArticle)

        val poller = SourcePoller(rssFeedFetcher, websiteFetcher, twitterFetcher, articleRepository, sourceRepository, appProperties())
        poller.poll(source)

        verify { articleRepository.save(any()) }
    }

    @Test
    fun `respects custom max article age`() {
        val article10DaysOld = article(publishedAt = Instant.now().minus(10, ChronoUnit.DAYS).toString())
        every { rssFeedFetcher.fetch(any(), any(), any()) } returns listOf(article10DaysOld)

        val pollerWith7Days = SourcePoller(rssFeedFetcher, websiteFetcher, twitterFetcher, articleRepository, sourceRepository, appProperties(maxArticleAgeDays = 7))
        pollerWith7Days.poll(source)
        verify(exactly = 0) { articleRepository.save(match { it.title == "Test Article" }) }

        val pollerWith14Days = SourcePoller(rssFeedFetcher, websiteFetcher, twitterFetcher, articleRepository, sourceRepository, appProperties(maxArticleAgeDays = 14))
        pollerWith14Days.poll(source)
        verify { articleRepository.save(any()) }
    }

    @Test
    fun `dispatches twitter source to twitterFetcher with userId`() {
        val twitterSource = Source(id = "s2", podcastId = "p1", type = "twitter", url = "testuser")
        val tweetArticle = Article(
            sourceId = "s2",
            title = "Tweet text",
            body = "Tweet text",
            url = "https://x.com/testuser/status/123",
            publishedAt = Instant.now().toString(),
            author = "@testuser",
            contentHash = ""
        )
        every { twitterFetcher.fetch("testuser", "s2", null, "user1") } returns listOf(tweetArticle)
        every { twitterFetcher.buildLastSeenId(null, any(), "testuser", "user1") } returns "999:123"

        val poller = SourcePoller(rssFeedFetcher, websiteFetcher, twitterFetcher, articleRepository, sourceRepository, appProperties())
        poller.poll(twitterSource, userId = "user1")

        verify { articleRepository.save(any()) }
        verify { twitterFetcher.fetch("testuser", "s2", null, "user1") }
    }

    @Test
    fun `skips twitter source when no userId provided`() {
        val twitterSource = Source(id = "s2", podcastId = "p1", type = "twitter", url = "testuser")

        val poller = SourcePoller(rssFeedFetcher, websiteFetcher, twitterFetcher, articleRepository, sourceRepository, appProperties())
        poller.poll(twitterSource, userId = null)

        verify(exactly = 0) { twitterFetcher.fetch(any(), any(), any(), any()) }
    }
}

package com.aisummarypodcast.source

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatusCode
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException

class TwitterFetcherTest {

    private val xTokenManager = mockk<XTokenManager>()
    private val xClient = mockk<XClient>()
    private val fetcher = TwitterFetcher(xTokenManager, xClient)

    @Test
    fun `extractUsername from plain username`() {
        assertEquals("elonmusk", fetcher.extractUsername("elonmusk"))
    }

    @Test
    fun `extractUsername from @username`() {
        assertEquals("elonmusk", fetcher.extractUsername("@elonmusk"))
    }

    @Test
    fun `extractUsername from x dot com URL`() {
        assertEquals("elonmusk", fetcher.extractUsername("https://x.com/elonmusk"))
    }

    @Test
    fun `extractUsername from twitter dot com URL`() {
        assertEquals("elonmusk", fetcher.extractUsername("https://twitter.com/elonmusk"))
    }

    @Test
    fun `extractUsername from URL with trailing slash`() {
        assertEquals("elonmusk", fetcher.extractUsername("https://x.com/elonmusk/"))
    }

    @Test
    fun `parseLastSeenId with userId and sinceId`() {
        val (userId, sinceId) = fetcher.parseLastSeenId("12345:67890")
        assertEquals("12345", userId)
        assertEquals("67890", sinceId)
    }

    @Test
    fun `parseLastSeenId with null`() {
        val (userId, sinceId) = fetcher.parseLastSeenId(null)
        assertEquals(null, userId)
        assertEquals(null, sinceId)
    }

    @Test
    fun `parseLastSeenId with invalid format`() {
        val (userId, sinceId) = fetcher.parseLastSeenId("no-colon-here")
        assertEquals(null, userId)
        assertEquals(null, sinceId)
    }

    @Test
    fun `mapTweetToArticle with short text`() {
        val tweet = XTweet(id = "123", text = "Short tweet", createdAt = "2026-02-15T10:00:00.000Z")
        val article = fetcher.mapTweetToArticle(tweet, "testuser", "s1")

        assertEquals("Short tweet", article.title)
        assertEquals("Short tweet", article.body)
        assertEquals("https://x.com/testuser/status/123", article.url)
        assertEquals("@testuser", article.author)
        assertEquals("s1", article.sourceId)
    }

    @Test
    fun `mapTweetToArticle with long text truncates title`() {
        val longText = "A".repeat(150)
        val tweet = XTweet(id = "456", text = longText, createdAt = "2026-02-15T10:00:00.000Z")
        val article = fetcher.mapTweetToArticle(tweet, "testuser", "s1")

        assertEquals("A".repeat(100) + "...", article.title)
        assertEquals(longText, article.body)
    }

    @Test
    fun `fetch resolves username and fetches timeline on first poll`() {
        every { xTokenManager.getValidAccessToken("user1") } returns "access-token"
        every { xClient.resolveUsername("access-token", "testuser") } returns XUserData(id = "999", username = "testuser")
        every { xClient.getUserTimeline("access-token", "999", null) } returns listOf(
            XTweet(id = "111", text = "Hello world", createdAt = "2026-02-15T10:00:00.000Z")
        )

        val articles = fetcher.fetch("testuser", "s1", null, "user1")

        assertEquals(1, articles.size)
        assertEquals("Hello world", articles[0].body)
        verify { xClient.resolveUsername("access-token", "testuser") }
    }

    @Test
    fun `fetch uses cached user ID on subsequent poll`() {
        every { xTokenManager.getValidAccessToken("user1") } returns "access-token"
        every { xClient.getUserTimeline("access-token", "999", "100") } returns listOf(
            XTweet(id = "200", text = "New tweet", createdAt = "2026-02-15T12:00:00.000Z")
        )

        val articles = fetcher.fetch("testuser", "s1", "999:100", "user1")

        assertEquals(1, articles.size)
        verify(exactly = 0) { xClient.resolveUsername(any(), any()) }
    }

    @Test
    fun `fetch returns empty list on rate limit`() {
        every { xTokenManager.getValidAccessToken("user1") } returns "access-token"
        every { xClient.resolveUsername("access-token", "testuser") } returns XUserData(id = "999", username = "testuser")
        every { xClient.getUserTimeline("access-token", "999", null) } throws
            HttpClientErrorException(HttpStatusCode.valueOf(429))
        every { xClient.isRateLimited(any()) } returns true
        every { xClient.isAuthError(any()) } returns false
        every { xClient.isNotFound(any()) } returns false

        val articles = fetcher.fetch("testuser", "s1", null, "user1")

        assertEquals(0, articles.size)
    }

    @Test
    fun `fetch returns empty list on auth error`() {
        every { xTokenManager.getValidAccessToken("user1") } returns "access-token"
        every { xClient.resolveUsername("access-token", "testuser") } returns XUserData(id = "999", username = "testuser")
        every { xClient.getUserTimeline("access-token", "999", null) } throws
            HttpClientErrorException(HttpStatusCode.valueOf(401))
        every { xClient.isRateLimited(any()) } returns false
        every { xClient.isAuthError(any()) } returns true
        every { xClient.isNotFound(any()) } returns false

        val articles = fetcher.fetch("testuser", "s1", null, "user1")

        assertEquals(0, articles.size)
    }

    @Test
    fun `fetch returns empty list on user not found`() {
        every { xTokenManager.getValidAccessToken("user1") } returns "access-token"
        every { xClient.resolveUsername("access-token", "testuser") } throws
            HttpClientErrorException(HttpStatusCode.valueOf(404))
        every { xClient.isRateLimited(any()) } returns false
        every { xClient.isAuthError(any()) } returns false
        every { xClient.isNotFound(any()) } returns true

        val articles = fetcher.fetch("testuser", "s1", null, "user1")

        assertEquals(0, articles.size)
    }

    @Test
    fun `fetch returns empty list when no OAuth connection`() {
        every { xTokenManager.getValidAccessToken("user1") } throws
            IllegalStateException("No X connection found for user user1")

        val articles = fetcher.fetch("testuser", "s1", null, "user1")

        assertEquals(0, articles.size)
    }

    @Test
    fun `fetch returns empty list on server error`() {
        every { xTokenManager.getValidAccessToken("user1") } returns "access-token"
        every { xClient.resolveUsername("access-token", "testuser") } returns XUserData(id = "999", username = "testuser")
        every { xClient.getUserTimeline("access-token", "999", null) } throws
            HttpServerErrorException(HttpStatusCode.valueOf(500))

        val articles = fetcher.fetch("testuser", "s1", null, "user1")

        assertEquals(0, articles.size)
    }

    @Test
    fun `buildLastSeenId creates userId colon tweetId format`() {
        val articles = listOf(
            fetcher.mapTweetToArticle(XTweet(id = "100", text = "older"), "user", "s1"),
            fetcher.mapTweetToArticle(XTweet(id = "200", text = "newer"), "user", "s1")
        )

        val result = fetcher.buildLastSeenId("999:50", articles, "user", "user1")

        assertEquals("999:200", result)
    }

    @Test
    fun `buildLastSeenId returns current value when no new articles`() {
        val result = fetcher.buildLastSeenId("999:50", emptyList(), "user", "user1")

        assertEquals("999:50", result)
    }
}

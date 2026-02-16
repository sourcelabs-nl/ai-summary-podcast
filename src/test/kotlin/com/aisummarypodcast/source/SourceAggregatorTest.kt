package com.aisummarypodcast.source

import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.Source
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SourceAggregatorTest {

    private val aggregator = SourceAggregator()

    private fun source(
        type: String = "rss",
        url: String = "https://nitter.net/simonw/rss",
        aggregate: Boolean? = null
    ) = Source(id = "s1", podcastId = "p1", type = type, url = url, aggregate = aggregate)

    private fun article(
        body: String = "Hello world",
        author: String? = "@simonw",
        publishedAt: String? = "2026-02-16T10:00:00Z"
    ) = Article(sourceId = "s1", title = body.take(50), body = body, url = "https://nitter.net/simonw/status/123", publishedAt = publishedAt, author = author, contentHash = "")

    // --- Aggregation behavior ---

    @Test
    fun `multiple articles aggregated into single digest`() {
        val articles = listOf(
            article(body = "First tweet", publishedAt = "2026-02-16T09:00:00Z"),
            article(body = "Second tweet", publishedAt = "2026-02-16T10:00:00Z")
        )

        val result = aggregator.aggregate(articles, source())

        assertEquals(1, result.size)
        assertTrue(result[0].body.contains("First tweet"))
        assertTrue(result[0].body.contains("Second tweet"))
        assertTrue(result[0].body.contains("---"))
    }

    @Test
    fun `single article returned unchanged`() {
        val articles = listOf(article(body = "Only tweet"))

        val result = aggregator.aggregate(articles, source())

        assertEquals(1, result.size)
        assertEquals("Only tweet", result[0].body)
    }

    @Test
    fun `empty list returned unchanged`() {
        val result = aggregator.aggregate(emptyList(), source())

        assertTrue(result.isEmpty())
    }

    // --- Digest format ---

    @Test
    fun `digest title includes author and date`() {
        val articles = listOf(
            article(body = "Tweet 1", author = "@simonw", publishedAt = "2026-02-16T10:00:00Z"),
            article(body = "Tweet 2", author = "@simonw", publishedAt = "2026-02-16T09:00:00Z")
        )

        val result = aggregator.aggregate(articles, source())

        assertEquals("Posts from @simonw — Feb 16, 2026", result[0].title)
    }

    @Test
    fun `digest title uses domain when no author`() {
        val articles = listOf(
            article(body = "Tweet 1", author = null),
            article(body = "Tweet 2", author = null)
        )

        val result = aggregator.aggregate(articles, source())

        assertTrue(result[0].title.startsWith("Posts from nitter.net"))
    }

    @Test
    fun `digest publishedAt uses most recent`() {
        val articles = listOf(
            article(body = "Older", publishedAt = "2026-02-16T09:00:00Z"),
            article(body = "Newer", publishedAt = "2026-02-16T10:00:00Z")
        )

        val result = aggregator.aggregate(articles, source())

        assertEquals("2026-02-16T10:00:00Z", result[0].publishedAt)
    }

    @Test
    fun `digest author is first article author`() {
        val articles = listOf(
            article(body = "Tweet 1", author = "@simonw"),
            article(body = "Tweet 2", author = "@other")
        )

        val result = aggregator.aggregate(articles, source())

        assertEquals("@simonw", result[0].author)
    }

    @Test
    fun `digest author is null when no articles have author`() {
        val articles = listOf(
            article(body = "Tweet 1", author = null),
            article(body = "Tweet 2", author = null)
        )

        val result = aggregator.aggregate(articles, source())

        assertNull(result[0].author)
    }

    @Test
    fun `digest body prefixes each item with timestamp`() {
        val articles = listOf(
            article(body = "First", publishedAt = "2026-02-16T09:00:00Z"),
            article(body = "Second", publishedAt = "2026-02-16T10:00:00Z")
        )

        val result = aggregator.aggregate(articles, source())

        assertTrue(result[0].body.contains("2026-02-16T09:00:00Z\nFirst"))
        assertTrue(result[0].body.contains("2026-02-16T10:00:00Z\nSecond"))
    }

    @Test
    fun `digest url is the source url`() {
        val articles = listOf(article(body = "Tweet 1"), article(body = "Tweet 2"))

        val result = aggregator.aggregate(articles, source(url = "https://nitter.net/simonw/rss"))

        assertEquals("https://nitter.net/simonw/rss", result[0].url)
    }

    // --- Hybrid detection ---

    @Test
    fun `shouldAggregate returns true for twitter type with null override`() {
        assertTrue(aggregator.shouldAggregate(source(type = "twitter", url = "simonw", aggregate = null)))
    }

    @Test
    fun `shouldAggregate returns true for nitter URL with null override`() {
        assertTrue(aggregator.shouldAggregate(source(type = "rss", url = "https://nitter.net/user/rss", aggregate = null)))
    }

    @Test
    fun `shouldAggregate returns false for regular RSS with null override`() {
        assertFalse(aggregator.shouldAggregate(source(type = "rss", url = "https://example.com/feed.xml", aggregate = null)))
    }

    @Test
    fun `shouldAggregate returns true when explicit override is true`() {
        assertTrue(aggregator.shouldAggregate(source(type = "rss", url = "https://example.com/feed.xml", aggregate = true)))
    }

    @Test
    fun `shouldAggregate returns false when explicit override is false`() {
        assertFalse(aggregator.shouldAggregate(source(type = "twitter", url = "simonw", aggregate = false)))
    }

    @Test
    fun `articles not aggregated for regular RSS source`() {
        val articles = listOf(
            article(body = "Post 1"),
            article(body = "Post 2")
        )

        val result = aggregator.aggregate(articles, source(type = "rss", url = "https://example.com/feed.xml"))

        assertEquals(2, result.size)
    }
}

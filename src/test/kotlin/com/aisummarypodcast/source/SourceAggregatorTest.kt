package com.aisummarypodcast.source

import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.Post
import com.aisummarypodcast.store.PostArticle
import com.aisummarypodcast.store.PostArticleRepository
import com.aisummarypodcast.store.Source
import com.aisummarypodcast.store.SourceType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SourceAggregatorTest {

    private val articleRepository = mockk<ArticleRepository> {
        every { findBySourceIdAndContentHash(any(), any()) } returns null
        every { save(any()) } answers {
            val article = firstArg<Article>()
            article.copy(id = (Math.random() * 10000).toLong())
        }
    }
    private val postArticleRepository = mockk<PostArticleRepository> {
        every { save(any()) } answers { firstArg() }
    }

    private val aggregator = SourceAggregator(articleRepository, postArticleRepository)

    private fun source(
        type: SourceType = SourceType.RSS,
        url: String = "https://nitter.net/simonw/rss",
        aggregate: Boolean? = null
    ) = Source(id = "s1", podcastId = "p1", type = type, url = url, aggregate = aggregate)

    private fun post(
        id: Long = 1L,
        body: String = "Hello world",
        author: String? = "@simonw",
        publishedAt: String? = "2026-02-16T10:00:00Z"
    ) = Post(
        id = id,
        sourceId = "s1",
        title = body.take(50),
        body = body,
        url = "https://nitter.net/simonw/status/123",
        publishedAt = publishedAt,
        author = author,
        contentHash = "hash$id",
        createdAt = "2026-02-16T10:00:00Z"
    )

    // --- Aggregation behavior ---

    @Test
    fun `multiple posts aggregated into single article`() {
        val posts = listOf(
            post(id = 1, body = "First tweet", publishedAt = "2026-02-16T09:00:00Z"),
            post(id = 2, body = "Second tweet", publishedAt = "2026-02-16T10:00:00Z")
        )

        val result = aggregator.aggregateAndPersist(posts, source())

        assertEquals(1, result.size)
        assertTrue(result[0].body.contains("First tweet"))
        assertTrue(result[0].body.contains("Second tweet"))
        assertTrue(result[0].body.contains("---"))
    }

    @Test
    fun `single post returns individual article`() {
        val posts = listOf(post(id = 1, body = "Only tweet"))

        val result = aggregator.aggregateAndPersist(posts, source())

        assertEquals(1, result.size)
        assertEquals("Only tweet", result[0].body)
    }

    @Test
    fun `empty list returns empty`() {
        val result = aggregator.aggregateAndPersist(emptyList(), source())

        assertTrue(result.isEmpty())
    }

    // --- Digest format ---

    @Test
    fun `digest title includes author and date`() {
        val posts = listOf(
            post(id = 1, body = "Tweet 1", author = "@simonw", publishedAt = "2026-02-16T10:00:00Z"),
            post(id = 2, body = "Tweet 2", author = "@simonw", publishedAt = "2026-02-16T09:00:00Z")
        )

        val result = aggregator.aggregateAndPersist(posts, source())

        assertEquals("Posts from @simonw — Feb 16, 2026", result[0].title)
    }

    @Test
    fun `digest title uses domain when no author`() {
        val posts = listOf(
            post(id = 1, body = "Tweet 1", author = null),
            post(id = 2, body = "Tweet 2", author = null)
        )

        val result = aggregator.aggregateAndPersist(posts, source())

        assertTrue(result[0].title.startsWith("Posts from nitter.net"))
    }

    @Test
    fun `digest publishedAt uses most recent`() {
        val posts = listOf(
            post(id = 1, body = "Older", publishedAt = "2026-02-16T09:00:00Z"),
            post(id = 2, body = "Newer", publishedAt = "2026-02-16T10:00:00Z")
        )

        val result = aggregator.aggregateAndPersist(posts, source())

        assertEquals("2026-02-16T10:00:00Z", result[0].publishedAt)
    }

    @Test
    fun `digest body prefixes each item with timestamp`() {
        val posts = listOf(
            post(id = 1, body = "First", publishedAt = "2026-02-16T09:00:00Z"),
            post(id = 2, body = "Second", publishedAt = "2026-02-16T10:00:00Z")
        )

        val result = aggregator.aggregateAndPersist(posts, source())

        assertTrue(result[0].body.contains("2026-02-16T09:00:00Z\nFirst"))
        assertTrue(result[0].body.contains("2026-02-16T10:00:00Z\nSecond"))
    }

    // --- Post-article linkage ---

    @Test
    fun `aggregated posts create post_articles entries for all posts`() {
        val posts = listOf(
            post(id = 1, body = "Tweet 1"),
            post(id = 2, body = "Tweet 2"),
            post(id = 3, body = "Tweet 3")
        )

        aggregator.aggregateAndPersist(posts, source())

        verify(exactly = 3) { postArticleRepository.save(any()) }
    }

    @Test
    fun `non-aggregated posts create individual post_articles entries`() {
        val posts = listOf(
            post(id = 1, body = "Article 1"),
            post(id = 2, body = "Article 2")
        )

        aggregator.aggregateAndPersist(posts, source(type = SourceType.RSS, url = "https://example.com/feed.xml"))

        verify(exactly = 2) { postArticleRepository.save(any()) }
        verify(exactly = 2) { articleRepository.save(any()) }
    }

    @Test
    fun `non-aggregated source creates individual articles`() {
        val posts = listOf(
            post(id = 1, body = "Post 1"),
            post(id = 2, body = "Post 2")
        )

        val result = aggregator.aggregateAndPersist(posts, source(type = SourceType.RSS, url = "https://example.com/feed.xml"))

        assertEquals(2, result.size)
    }

    // --- Hybrid detection ---

    @Test
    fun `shouldAggregate returns true for twitter type with null override`() {
        assertTrue(aggregator.shouldAggregate(source(type = SourceType.TWITTER, url = "simonw", aggregate = null)))
    }

    @Test
    fun `shouldAggregate returns true for nitter URL with null override`() {
        assertTrue(aggregator.shouldAggregate(source(type = SourceType.RSS, url = "https://nitter.net/user/rss", aggregate = null)))
    }

    @Test
    fun `shouldAggregate returns false for regular RSS with null override`() {
        assertFalse(aggregator.shouldAggregate(source(type = SourceType.RSS, url = "https://example.com/feed.xml", aggregate = null)))
    }

    @Test
    fun `shouldAggregate returns true when explicit override is true`() {
        assertTrue(aggregator.shouldAggregate(source(type = SourceType.RSS, url = "https://example.com/feed.xml", aggregate = true)))
    }

    @Test
    fun `shouldAggregate returns false when explicit override is false`() {
        assertFalse(aggregator.shouldAggregate(source(type = SourceType.TWITTER, url = "simonw", aggregate = false)))
    }
}

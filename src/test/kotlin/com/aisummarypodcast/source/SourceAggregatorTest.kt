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
        title: String? = null,
        body: String = "Hello world",
        author: String? = "@simonw",
        publishedAt: String? = "2026-02-16T10:00:00Z",
        url: String = "https://nitter.net/simonw/status/123#m"
    ) = Post(
        id = id,
        sourceId = "s1",
        title = title ?: body.take(50),
        body = body,
        url = url,
        publishedAt = publishedAt,
        author = author,
        contentHash = "hash$id",
        createdAt = "2026-02-16T10:00:00Z"
    )

    // --- Thread detection ---

    @Test
    fun `groupPostsByThread creates single thread from parent and replies`() {
        val posts = listOf(
            post(id = 1, title = "Parent post", publishedAt = "2026-03-26T17:00:00Z"),
            post(id = 2, title = "R to @user: reply 1", publishedAt = "2026-03-26T17:00:01Z"),
            post(id = 3, title = "R to @user: reply 2", publishedAt = "2026-03-26T17:00:02Z")
        )

        val threads = aggregator.groupPostsByThread(posts)

        assertEquals(1, threads.size)
        assertEquals(3, threads[0].size)
        assertEquals("Parent post", threads[0][0].title)
    }

    @Test
    fun `groupPostsByThread creates multiple threads`() {
        val posts = listOf(
            post(id = 1, title = "Thread A", publishedAt = "2026-03-26T10:00:00Z"),
            post(id = 2, title = "R to @user: A reply", publishedAt = "2026-03-26T10:00:01Z"),
            post(id = 3, title = "Thread B", publishedAt = "2026-03-26T15:00:00Z"),
            post(id = 4, title = "R to @user: B reply", publishedAt = "2026-03-26T15:00:01Z")
        )

        val threads = aggregator.groupPostsByThread(posts)

        assertEquals(2, threads.size)
        assertEquals(2, threads[0].size)
        assertEquals("Thread A", threads[0][0].title)
        assertEquals(2, threads[1].size)
        assertEquals("Thread B", threads[1][0].title)
    }

    @Test
    fun `groupPostsByThread handles orphan reply as standalone thread`() {
        val posts = listOf(
            post(id = 1, title = "R to @user: orphan reply", publishedAt = "2026-03-26T10:00:00Z"),
            post(id = 2, title = "Normal post", publishedAt = "2026-03-26T15:00:00Z")
        )

        val threads = aggregator.groupPostsByThread(posts)

        assertEquals(2, threads.size)
        assertEquals(1, threads[0].size)
        assertEquals("R to @user: orphan reply", threads[0][0].title)
    }

    @Test
    fun `groupPostsByThread handles standalone posts`() {
        val posts = listOf(
            post(id = 1, title = "Standalone A", publishedAt = "2026-03-26T10:00:00Z"),
            post(id = 2, title = "Standalone B", publishedAt = "2026-03-26T15:00:00Z")
        )

        val threads = aggregator.groupPostsByThread(posts)

        assertEquals(2, threads.size)
        assertEquals(1, threads[0].size)
        assertEquals(1, threads[1].size)
    }

    @Test
    fun `groupPostsByThread sorts by publishedAt`() {
        val posts = listOf(
            post(id = 2, title = "Later post", publishedAt = "2026-03-26T15:00:00Z"),
            post(id = 1, title = "Earlier post", publishedAt = "2026-03-26T10:00:00Z")
        )

        val threads = aggregator.groupPostsByThread(posts)

        assertEquals(2, threads.size)
        assertEquals("Earlier post", threads[0][0].title)
        assertEquals("Later post", threads[1][0].title)
    }

    @Test
    fun `groupPostsByThread mixed threads and standalone`() {
        val posts = listOf(
            post(id = 1, title = "Standalone", publishedAt = "2026-03-25T10:00:00Z"),
            post(id = 2, title = "Thread parent", publishedAt = "2026-03-26T10:00:00Z"),
            post(id = 3, title = "R to @user: reply", publishedAt = "2026-03-26T10:00:01Z"),
            post(id = 4, title = "Another standalone", publishedAt = "2026-03-27T10:00:00Z")
        )

        val threads = aggregator.groupPostsByThread(posts)

        assertEquals(3, threads.size)
        assertEquals(1, threads[0].size)
        assertEquals(2, threads[1].size)
        assertEquals(1, threads[2].size)
    }

    // --- Nitter URL rewriting ---

    @Test
    fun `rewriteNitterUrl rewrites nitter to x dot com`() {
        assertEquals(
            "https://x.com/user/status/12345#m",
            aggregator.rewriteNitterUrl("https://nitter.net/user/status/12345#m")
        )
    }

    @Test
    fun `rewriteNitterUrl leaves non-nitter URLs unchanged`() {
        assertEquals(
            "https://example.com/post/123",
            aggregator.rewriteNitterUrl("https://example.com/post/123")
        )
    }

    @Test
    fun `rewriteNitterUrl handles malformed URLs gracefully`() {
        assertEquals("not a url", aggregator.rewriteNitterUrl("not a url"))
    }

    // --- Thread-based article creation ---

    @Test
    fun `thread-based aggregation creates one article per thread`() {
        val posts = listOf(
            post(id = 1, title = "Thread A parent", body = "Content A", publishedAt = "2026-03-26T10:00:00Z",
                url = "https://nitter.net/user/status/100#m"),
            post(id = 2, title = "R to @user: A reply", body = "Reply A", publishedAt = "2026-03-26T10:00:01Z",
                url = "https://nitter.net/user/status/101#m"),
            post(id = 3, title = "Thread B parent", body = "Content B", publishedAt = "2026-03-26T15:00:00Z",
                url = "https://nitter.net/user/status/200#m")
        )

        val result = aggregator.aggregateAndPersist(posts, source())

        assertEquals(2, result.size)
        assertEquals("Thread A parent", result[0].title)
        assertEquals("Thread B parent", result[1].title)
    }

    @Test
    fun `thread article URL uses parent post URL rewritten to x dot com`() {
        val posts = listOf(
            post(id = 1, title = "Parent", body = "Content", publishedAt = "2026-03-26T10:00:00Z",
                url = "https://nitter.net/user/status/100#m"),
            post(id = 2, title = "R to @user: reply", body = "Reply", publishedAt = "2026-03-26T10:00:01Z",
                url = "https://nitter.net/user/status/101#m")
        )

        val result = aggregator.aggregateAndPersist(posts, source())

        assertEquals(1, result.size)
        assertEquals("https://x.com/user/status/100#m", result[0].url)
    }

    @Test
    fun `thread article body combines parent and replies`() {
        val posts = listOf(
            post(id = 1, title = "Parent", body = "Parent content", publishedAt = "2026-03-26T10:00:00Z"),
            post(id = 2, title = "R to @user: reply", body = "Reply content", publishedAt = "2026-03-26T10:00:01Z")
        )

        val result = aggregator.aggregateAndPersist(posts, source())

        assertTrue(result[0].body.contains("Parent content"))
        assertTrue(result[0].body.contains("Reply content"))
        assertTrue(result[0].body.contains("---"))
    }

    @Test
    fun `thread article links all posts via post_articles`() {
        val posts = listOf(
            post(id = 1, title = "Parent", publishedAt = "2026-03-26T10:00:00Z"),
            post(id = 2, title = "R to @user: reply 1", publishedAt = "2026-03-26T10:00:01Z"),
            post(id = 3, title = "R to @user: reply 2", publishedAt = "2026-03-26T10:00:02Z")
        )

        aggregator.aggregateAndPersist(posts, source())

        // 3 posts linked to 1 article
        verify(exactly = 3) { postArticleRepository.save(any()) }
    }

    @Test
    fun `multiple threads link posts to correct articles`() {
        val posts = listOf(
            post(id = 1, title = "Thread A", publishedAt = "2026-03-26T10:00:00Z"),
            post(id = 2, title = "Thread B", publishedAt = "2026-03-26T15:00:00Z")
        )

        aggregator.aggregateAndPersist(posts, source())

        // 2 articles, 1 post each
        verify(exactly = 2) { articleRepository.save(any()) }
        verify(exactly = 2) { postArticleRepository.save(any()) }
    }

    // --- Single post and empty ---

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

    // --- Non-aggregated source ---

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

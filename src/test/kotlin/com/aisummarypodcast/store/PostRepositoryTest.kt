package com.aisummarypodcast.store

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant
import java.time.temporal.ChronoUnit

@SpringBootTest
class PostRepositoryTest {

    @Autowired lateinit var postRepository: PostRepository
    @Autowired lateinit var postArticleRepository: PostArticleRepository
    @Autowired lateinit var articleRepository: ArticleRepository
    @Autowired lateinit var sourceRepository: SourceRepository
    @Autowired lateinit var podcastRepository: PodcastRepository
    @Autowired lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        postArticleRepository.deleteAll()
        postRepository.deleteAll()
        articleRepository.deleteAll()
        sourceRepository.deleteAll()
        podcastRepository.deleteAll()
        userRepository.deleteAll()

        userRepository.save(User(id = "u1", name = "Test User"))
        podcastRepository.save(Podcast(id = "p1", userId = "u1", name = "Test", topic = "tech"))
        sourceRepository.save(Source(id = "s1", podcastId = "p1", type = "rss", url = "https://example.com/feed"))
        sourceRepository.save(Source(id = "s2", podcastId = "p1", type = "twitter", url = "testuser"))
    }

    private fun post(
        sourceId: String = "s1",
        body: String = "Post body",
        contentHash: String = "hash1",
        createdAt: String = Instant.now().toString()
    ) = Post(
        sourceId = sourceId,
        title = "Test Post",
        body = body,
        url = "https://example.com/post",
        contentHash = contentHash,
        createdAt = createdAt
    )

    // --- Deduplication ---

    @Test
    fun `findBySourceIdAndContentHash returns matching post`() {
        postRepository.save(post(contentHash = "abc123"))

        val found = postRepository.findBySourceIdAndContentHash("s1", "abc123")

        assertNotNull(found)
        assertEquals("abc123", found!!.contentHash)
    }

    @Test
    fun `findBySourceIdAndContentHash returns null for unknown hash`() {
        postRepository.save(post(contentHash = "abc123"))

        val found = postRepository.findBySourceIdAndContentHash("s1", "unknown")

        assertNull(found)
    }

    @Test
    fun `posts from different sources with same hash are stored independently`() {
        postRepository.save(post(sourceId = "s1", contentHash = "samehash"))
        postRepository.save(post(sourceId = "s2", contentHash = "samehash"))

        assertNotNull(postRepository.findBySourceIdAndContentHash("s1", "samehash"))
        assertNotNull(postRepository.findBySourceIdAndContentHash("s2", "samehash"))
    }

    // --- Unlinked post queries ---

    @Test
    fun `findUnlinkedBySourceIds returns posts with no post_articles entry`() {
        val saved = postRepository.save(post(createdAt = Instant.now().toString()))

        val cutoff = Instant.now().minus(7, ChronoUnit.DAYS).toString()
        val unlinked = postRepository.findUnlinkedBySourceIds(listOf("s1"), cutoff)

        assertEquals(1, unlinked.size)
        assertEquals(saved.id, unlinked[0].id)
    }

    @Test
    fun `findUnlinkedBySourceIds excludes linked posts`() {
        val saved = postRepository.save(post(createdAt = Instant.now().toString()))
        val article = articleRepository.save(Article(sourceId = "s1", title = "Art", body = "body", url = "https://example.com", contentHash = "arthash"))
        postArticleRepository.save(PostArticle(postId = saved.id!!, articleId = article.id!!))

        val cutoff = Instant.now().minus(7, ChronoUnit.DAYS).toString()
        val unlinked = postRepository.findUnlinkedBySourceIds(listOf("s1"), cutoff)

        assertEquals(0, unlinked.size)
    }

    @Test
    fun `findUnlinkedBySourceIds excludes posts outside time window`() {
        postRepository.save(post(createdAt = Instant.now().minus(10, ChronoUnit.DAYS).toString()))

        val cutoff = Instant.now().minus(7, ChronoUnit.DAYS).toString()
        val unlinked = postRepository.findUnlinkedBySourceIds(listOf("s1"), cutoff)

        assertEquals(0, unlinked.size)
    }

    @Test
    fun `findUnlinkedBySourceIds filters by source IDs`() {
        postRepository.save(post(sourceId = "s1", contentHash = "h1", createdAt = Instant.now().toString()))
        postRepository.save(post(sourceId = "s2", contentHash = "h2", createdAt = Instant.now().toString()))

        val cutoff = Instant.now().minus(7, ChronoUnit.DAYS).toString()
        val unlinked = postRepository.findUnlinkedBySourceIds(listOf("s1"), cutoff)

        assertEquals(1, unlinked.size)
        assertEquals("s1", unlinked[0].sourceId)
    }

    // --- Cleanup ---

    @Test
    fun `deleteOldUnlinkedPosts deletes old posts with no post_articles entry`() {
        postRepository.save(post(createdAt = Instant.now().minus(30, ChronoUnit.DAYS).toString()))

        val cutoff = Instant.now().minus(7, ChronoUnit.DAYS).toString()
        postRepository.deleteOldUnlinkedPosts(cutoff)

        assertEquals(0, postRepository.count())
    }

    @Test
    fun `deleteOldUnlinkedPosts retains old linked posts`() {
        val saved = postRepository.save(post(createdAt = Instant.now().minus(30, ChronoUnit.DAYS).toString()))
        val article = articleRepository.save(Article(sourceId = "s1", title = "Art", body = "body", url = "https://example.com", contentHash = "arthash"))
        postArticleRepository.save(PostArticle(postId = saved.id!!, articleId = article.id!!))

        val cutoff = Instant.now().minus(7, ChronoUnit.DAYS).toString()
        postRepository.deleteOldUnlinkedPosts(cutoff)

        assertEquals(1, postRepository.count())
    }

    @Test
    fun `deleteOldUnlinkedPosts retains recent unlinked posts`() {
        postRepository.save(post(createdAt = Instant.now().minus(2, ChronoUnit.DAYS).toString()))

        val cutoff = Instant.now().minus(7, ChronoUnit.DAYS).toString()
        postRepository.deleteOldUnlinkedPosts(cutoff)

        assertEquals(1, postRepository.count())
    }
}

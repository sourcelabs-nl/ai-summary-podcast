package com.aisummarypodcast.store

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant
import java.time.temporal.ChronoUnit

@SpringBootTest
class ArticleRepositoryTest {

    @Autowired lateinit var articleRepository: ArticleRepository
    @Autowired lateinit var sourceRepository: SourceRepository
    @Autowired lateinit var podcastRepository: PodcastRepository
    @Autowired lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        articleRepository.deleteAll()
        sourceRepository.deleteAll()
        podcastRepository.deleteAll()
        userRepository.deleteAll()

        userRepository.save(User(id = "u1", name = "Test User"))
        podcastRepository.save(Podcast(id = "p1", userId = "u1", name = "Test", topic = "tech"))
        sourceRepository.save(Source(id = "s1", podcastId = "p1", type = "rss", url = "https://example.com/feed"))
    }

    @Test
    fun `deleteOldUnprocessedArticles deletes old unprocessed articles`() {
        val oldDate = Instant.now().minus(30, ChronoUnit.DAYS).toString()
        articleRepository.save(Article(sourceId = "s1", title = "Old Unprocessed", body = "body", url = "https://example.com/1", publishedAt = oldDate, contentHash = "hash1", isProcessed = false))

        val cutoff = Instant.now().minus(7, ChronoUnit.DAYS).toString()
        articleRepository.deleteOldUnprocessedArticles(cutoff)

        assertEquals(0, articleRepository.count())
    }

    @Test
    fun `deleteOldUnprocessedArticles retains old processed articles`() {
        val oldDate = Instant.now().minus(30, ChronoUnit.DAYS).toString()
        articleRepository.save(Article(sourceId = "s1", title = "Old Processed", body = "body", url = "https://example.com/1", publishedAt = oldDate, contentHash = "hash1", isProcessed = true))

        val cutoff = Instant.now().minus(7, ChronoUnit.DAYS).toString()
        articleRepository.deleteOldUnprocessedArticles(cutoff)

        assertEquals(1, articleRepository.count())
    }

    @Test
    fun `deleteOldUnprocessedArticles retains recent unprocessed articles`() {
        val recentDate = Instant.now().minus(2, ChronoUnit.DAYS).toString()
        articleRepository.save(Article(sourceId = "s1", title = "Recent", body = "body", url = "https://example.com/1", publishedAt = recentDate, contentHash = "hash1", isProcessed = false))

        val cutoff = Instant.now().minus(7, ChronoUnit.DAYS).toString()
        articleRepository.deleteOldUnprocessedArticles(cutoff)

        assertEquals(1, articleRepository.count())
    }

    @Test
    fun `deleteOldUnprocessedArticles retains articles with null publishedAt`() {
        articleRepository.save(Article(sourceId = "s1", title = "No Date", body = "body", url = "https://example.com/1", publishedAt = null, contentHash = "hash1", isProcessed = false))

        val cutoff = Instant.now().minus(7, ChronoUnit.DAYS).toString()
        articleRepository.deleteOldUnprocessedArticles(cutoff)

        assertEquals(1, articleRepository.count())
    }
}

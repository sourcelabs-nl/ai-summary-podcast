package com.aisummarypodcast.podcast

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.BriefingProperties
import com.aisummarypodcast.config.EncryptionProperties
import com.aisummarypodcast.config.EpisodesProperties
import com.aisummarypodcast.config.FeedProperties
import com.aisummarypodcast.config.LlmProperties
import com.aisummarypodcast.config.SourceProperties
import com.aisummarypodcast.llm.LlmPipeline
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PodcastRepository
import com.aisummarypodcast.store.Post
import com.aisummarypodcast.store.PostRepository
import com.aisummarypodcast.store.Source
import com.aisummarypodcast.store.SourceRepository
import com.aisummarypodcast.store.SourceType
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PodcastServiceTest {

    private val podcastRepository = mockk<PodcastRepository>()
    private val sourceRepository = mockk<SourceRepository>()
    private val articleRepository = mockk<ArticleRepository>()
    private val postRepository = mockk<PostRepository>()
    private val episodeRepository = mockk<EpisodeRepository>()
    private val llmPipeline = mockk<LlmPipeline>()
    private val episodeService = mockk<EpisodeService>()
    private val appProperties = AppProperties(
        llm = LlmProperties(),
        briefing = BriefingProperties(),
        episodes = EpisodesProperties(),
        feed = FeedProperties(),
        encryption = EncryptionProperties(masterKey = "test-key"),
        source = SourceProperties(maxArticleAgeDays = 7)
    )

    private val podcastService = PodcastService(
        podcastRepository, sourceRepository, articleRepository, postRepository,
        episodeRepository, appProperties, llmPipeline, episodeService
    )

    private val podcast = Podcast(
        id = "p1", userId = "u1", name = "Test", topic = "tech",
        lastGeneratedAt = "2026-03-01T00:00:00Z"
    )

    private val source = Source(
        id = "s1", podcastId = "p1", type = SourceType.RSS,
        url = "https://example.com/feed", pollIntervalMinutes = 60
    )

    @Test
    fun `getUpcomingContent returns articles and unlinked posts since lastGeneratedAt`() {
        val article = Article(
            id = 1, sourceId = "s1", title = "Article 1", body = "body",
            url = "https://example.com/1", contentHash = "h1",
            publishedAt = "2026-03-02T00:00:00Z", relevanceScore = 7
        )
        val post = Post(
            id = 2, sourceId = "s1", title = "Post 1", body = "body",
            url = "https://example.com/2", contentHash = "h2",
            createdAt = "2026-03-02T00:00:00Z"
        )

        every { sourceRepository.findByPodcastId("p1") } returns listOf(source)
        every { articleRepository.findAllSince(listOf("s1"), "2026-03-01T00:00:00Z") } returns listOf(article)
        every { postRepository.findUnlinkedSince(listOf("s1"), "2026-03-01T00:00:00Z") } returns listOf(post)

        val result = podcastService.getUpcomingContent(podcast)

        assertEquals(1, result.articles.size)
        assertEquals(1, result.unlinkedPosts.size)
        assertEquals("Article 1", result.articles[0].title)
        assertEquals("Post 1", result.unlinkedPosts[0].title)
    }

    @Test
    fun `getUpcomingContent falls back to maxArticleAgeDays when lastGeneratedAt is null`() {
        val podcastNoGenerated = podcast.copy(lastGeneratedAt = null)

        every { sourceRepository.findByPodcastId("p1") } returns listOf(source)
        every { articleRepository.findAllSince(eq(listOf("s1")), any()) } returns emptyList()
        every { postRepository.findUnlinkedSince(eq(listOf("s1")), any()) } returns emptyList()

        val result = podcastService.getUpcomingContent(podcastNoGenerated)

        assertTrue(result.articles.isEmpty())
        assertTrue(result.unlinkedPosts.isEmpty())
    }

    @Test
    fun `getUpcomingContent returns empty when no sources`() {
        every { sourceRepository.findByPodcastId("p1") } returns emptyList()

        val result = podcastService.getUpcomingContent(podcast)

        assertTrue(result.articles.isEmpty())
        assertTrue(result.unlinkedPosts.isEmpty())
        assertTrue(result.sources.isEmpty())
    }
}

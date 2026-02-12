package com.aisummarypodcast.llm

import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.Source
import com.aisummarypodcast.store.SourceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LlmPipelineTest {

    private val articleProcessor = mockk<ArticleProcessor>()
    private val briefingComposer = mockk<BriefingComposer>()
    private val articleRepository = mockk<ArticleRepository> {
        every { save(any()) } answers { firstArg() }
    }
    private val sourceRepository = mockk<SourceRepository>()

    private val pipeline = LlmPipeline(
        articleProcessor, briefingComposer, articleRepository, sourceRepository
    )

    private val podcast = Podcast(id = "p1", userId = "u1", name = "Tech Daily", topic = "tech")

    @Test
    fun `returns null when podcast has no sources`() {
        every { sourceRepository.findByPodcastId("p1") } returns emptyList()

        val result = pipeline.run(podcast)

        assertNull(result)
    }

    @Test
    fun `returns null when no unfiltered articles exist`() {
        every { sourceRepository.findByPodcastId("p1") } returns listOf(
            Source(id = "s1", podcastId = "p1", type = "rss", url = "https://example.com/feed")
        )
        every { articleRepository.findUnfilteredBySourceIds(listOf("s1")) } returns emptyList()

        val result = pipeline.run(podcast)

        assertNull(result)
    }

    @Test
    fun `returns null when no articles are relevant`() {
        val source = Source(id = "s1", podcastId = "p1", type = "rss", url = "https://example.com/feed")
        val article = Article(
            id = 1, sourceId = "s1", title = "Random Article", body = "body",
            url = "https://example.com/1", contentHash = "hash1"
        )

        every { sourceRepository.findByPodcastId("p1") } returns listOf(source)
        every { articleRepository.findUnfilteredBySourceIds(listOf("s1")) } returns listOf(article)
        every { articleProcessor.process(listOf(article), podcast) } returns emptyList()

        val result = pipeline.run(podcast)

        assertNull(result)
    }

    @Test
    fun `marks articles as processed with summaries preserved`() {
        val source = Source(id = "s1", podcastId = "p1", type = "rss", url = "https://example.com/feed")
        val article = Article(
            id = 1, sourceId = "s1", title = "AI News", body = "Article body",
            url = "https://example.com/ai", contentHash = "hash1"
        )
        val processedArticle = article.copy(isRelevant = true, summary = "AI is advancing rapidly.")

        every { sourceRepository.findByPodcastId("p1") } returns listOf(source)
        every { articleRepository.findUnfilteredBySourceIds(listOf("s1")) } returns listOf(article)
        every { articleProcessor.process(listOf(article), podcast) } returns listOf(processedArticle)
        every { briefingComposer.compose(listOf(processedArticle), podcast) } returns "Today in tech..."

        val result = pipeline.run(podcast)

        assertNotNull(result)

        val savedArticle = slot<Article>()
        verify { articleRepository.save(capture(savedArticle)) }
        assertEquals(true, savedArticle.captured.isProcessed)
        assertEquals("AI is advancing rapidly.", savedArticle.captured.summary)
    }

    @Test
    fun `processes unfiltered articles and passes results to briefing composer`() {
        val source = Source(id = "s1", podcastId = "p1", type = "rss", url = "https://example.com/feed")
        val article = Article(
            id = 1, sourceId = "s1", title = "News", body = "body",
            url = "https://example.com/1", contentHash = "hash1"
        )
        val processedArticle = article.copy(isRelevant = true, summary = "Summary text.")

        every { sourceRepository.findByPodcastId("p1") } returns listOf(source)
        every { articleRepository.findUnfilteredBySourceIds(listOf("s1")) } returns listOf(article)
        every { articleProcessor.process(listOf(article), podcast) } returns listOf(processedArticle)
        every { briefingComposer.compose(listOf(processedArticle), podcast) } returns "Final script"

        val result = pipeline.run(podcast)

        assertEquals("Final script", result)
        verify { articleProcessor.process(listOf(article), podcast) }
        verify { briefingComposer.compose(listOf(processedArticle), podcast) }
    }
}

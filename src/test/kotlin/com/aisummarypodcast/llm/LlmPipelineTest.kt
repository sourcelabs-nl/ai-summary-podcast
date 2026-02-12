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

    private val relevanceFilter = mockk<RelevanceFilter>()
    private val articleSummarizer = mockk<ArticleSummarizer>()
    private val briefingComposer = mockk<BriefingComposer>()
    private val articleRepository = mockk<ArticleRepository> {
        every { save(any()) } answers { firstArg() }
    }
    private val sourceRepository = mockk<SourceRepository>()

    private val pipeline = LlmPipeline(
        relevanceFilter, articleSummarizer, briefingComposer, articleRepository, sourceRepository
    )

    private val podcast = Podcast(id = "p1", userId = "u1", name = "Tech Daily", topic = "tech")

    @Test
    fun `returns null when podcast has no sources`() {
        every { sourceRepository.findByPodcastId("p1") } returns emptyList()

        val result = pipeline.run(podcast)

        assertNull(result)
    }

    @Test
    fun `returns null when no relevant unprocessed articles exist`() {
        every { sourceRepository.findByPodcastId("p1") } returns listOf(
            Source(id = "s1", podcastId = "p1", type = "rss", url = "https://example.com/feed")
        )
        every { articleRepository.findUnfilteredBySourceIds(listOf("s1")) } returns emptyList()
        every { articleRepository.findRelevantUnprocessedBySourceIds(listOf("s1")) } returns emptyList()

        val result = pipeline.run(podcast)

        assertNull(result)
    }

    @Test
    fun `marks articles as processed with summaries preserved`() {
        val source = Source(id = "s1", podcastId = "p1", type = "rss", url = "https://example.com/feed")
        val article = Article(
            id = 1, sourceId = "s1", title = "AI News", body = "Article body",
            url = "https://example.com/ai", contentHash = "hash1", isRelevant = true
        )
        val summarizedArticle = article.copy(summary = "AI is advancing rapidly.")

        every { sourceRepository.findByPodcastId("p1") } returns listOf(source)
        every { articleRepository.findUnfilteredBySourceIds(listOf("s1")) } returns emptyList()
        every { articleRepository.findRelevantUnprocessedBySourceIds(listOf("s1")) } returns listOf(article)
        every { articleSummarizer.summarize(listOf(article), podcast) } returns listOf(summarizedArticle)
        every { briefingComposer.compose(listOf(summarizedArticle), podcast) } returns "Today in tech..."

        val result = pipeline.run(podcast)

        assertNotNull(result)

        val savedArticle = slot<Article>()
        verify { articleRepository.save(capture(savedArticle)) }
        assertEquals(true, savedArticle.captured.isProcessed)
        assertEquals("AI is advancing rapidly.", savedArticle.captured.summary)
    }

    @Test
    fun `filters unfiltered articles before summarizing`() {
        val source = Source(id = "s1", podcastId = "p1", type = "rss", url = "https://example.com/feed")
        val unfilteredArticle = Article(
            id = 1, sourceId = "s1", title = "Random Article", body = "body",
            url = "https://example.com/1", contentHash = "hash1"
        )
        val relevantArticle = Article(
            id = 2, sourceId = "s1", title = "Tech Article", body = "body",
            url = "https://example.com/2", contentHash = "hash2", isRelevant = true
        )
        val summarized = relevantArticle.copy(summary = "Tech summary.")

        every { sourceRepository.findByPodcastId("p1") } returns listOf(source)
        every { articleRepository.findUnfilteredBySourceIds(listOf("s1")) } returns listOf(unfilteredArticle)
        every { relevanceFilter.filter(listOf(unfilteredArticle), podcast) } returns listOf(unfilteredArticle)
        every { articleRepository.findRelevantUnprocessedBySourceIds(listOf("s1")) } returns listOf(relevantArticle)
        every { articleSummarizer.summarize(listOf(relevantArticle), podcast) } returns listOf(summarized)
        every { briefingComposer.compose(listOf(summarized), podcast) } returns "Script"

        pipeline.run(podcast)

        verify { relevanceFilter.filter(listOf(unfilteredArticle), podcast) }
        val savedArticle = slot<Article>()
        verify { articleRepository.save(capture(savedArticle)) }
        assertEquals("Tech summary.", savedArticle.captured.summary)
        assertEquals(true, savedArticle.captured.isProcessed)
    }

    @Test
    fun `passes summarized articles to briefing composer`() {
        val source = Source(id = "s1", podcastId = "p1", type = "rss", url = "https://example.com/feed")
        val article = Article(
            id = 1, sourceId = "s1", title = "News", body = "body",
            url = "https://example.com/1", contentHash = "hash1", isRelevant = true
        )
        val summarized = article.copy(summary = "Summary text.")

        every { sourceRepository.findByPodcastId("p1") } returns listOf(source)
        every { articleRepository.findUnfilteredBySourceIds(listOf("s1")) } returns emptyList()
        every { articleRepository.findRelevantUnprocessedBySourceIds(listOf("s1")) } returns listOf(article)
        every { articleSummarizer.summarize(listOf(article), podcast) } returns listOf(summarized)
        every { briefingComposer.compose(listOf(summarized), podcast) } returns "Final script"

        val result = pipeline.run(podcast)

        assertEquals("Final script", result)
        verify { briefingComposer.compose(listOf(summarized), podcast) }
    }
}
package com.aisummarypodcast.llm

import com.aisummarypodcast.config.ModelDefinition
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.Source
import com.aisummarypodcast.store.SourceRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LlmPipelineTest {

    private val relevanceScorer = mockk<RelevanceScorer>()
    private val articleSummarizer = mockk<ArticleSummarizer>()
    private val briefingComposer = mockk<BriefingComposer>()
    private val modelResolver = mockk<ModelResolver>()
    private val articleRepository = mockk<ArticleRepository> {
        every { save(any()) } answers { firstArg() }
    }
    private val sourceRepository = mockk<SourceRepository>()

    private val filterModelDef = ModelDefinition(provider = "openrouter", model = "anthropic/claude-haiku-4.5")
    private val composeModelDef = ModelDefinition(provider = "openrouter", model = "anthropic/claude-sonnet-4")

    private val pipeline = LlmPipeline(
        relevanceScorer, articleSummarizer, briefingComposer, modelResolver, articleRepository, sourceRepository
    )

    private val podcast = Podcast(id = "p1", userId = "u1", name = "Tech Daily", topic = "tech", relevanceThreshold = 5)

    @Test
    fun `returns null when podcast has no sources`() {
        every { sourceRepository.findByPodcastId("p1") } returns emptyList()

        val result = pipeline.run(podcast)

        assertNull(result)
    }

    @Test
    fun `returns null when no relevant unprocessed articles exist`() {
        val source = Source(id = "s1", podcastId = "p1", type = "rss", url = "https://example.com/feed")

        every { sourceRepository.findByPodcastId("p1") } returns listOf(source)
        every { modelResolver.resolve(podcast, "filter") } returns filterModelDef
        every { modelResolver.resolve(podcast, "compose") } returns composeModelDef
        every { articleRepository.findUnscoredBySourceIds(listOf("s1")) } returns emptyList()
        every { articleRepository.findRelevantUnsummarizedBySourceIds(listOf("s1"), 5) } returns emptyList()
        every { articleRepository.findRelevantUnprocessedBySourceIds(listOf("s1"), 5) } returns emptyList()

        val result = pipeline.run(podcast)

        assertNull(result)
    }

    @Test
    fun `scores unscored articles then summarizes and composes`() {
        val source = Source(id = "s1", podcastId = "p1", type = "rss", url = "https://example.com/feed")
        val unscoredArticle = Article(
            id = 1, sourceId = "s1", title = "AI News", body = "Article body",
            url = "https://example.com/ai", contentHash = "hash1"
        )
        val scoredArticle = unscoredArticle.copy(relevanceScore = 8)
        val summarizedArticle = scoredArticle.copy(summary = "AI is advancing rapidly.")

        every { sourceRepository.findByPodcastId("p1") } returns listOf(source)
        every { modelResolver.resolve(podcast, "filter") } returns filterModelDef
        every { modelResolver.resolve(podcast, "compose") } returns composeModelDef
        every { articleRepository.findUnscoredBySourceIds(listOf("s1")) } returns listOf(unscoredArticle)
        every { relevanceScorer.score(listOf(unscoredArticle), podcast, filterModelDef) } returns listOf(scoredArticle)
        every { articleRepository.findRelevantUnsummarizedBySourceIds(listOf("s1"), 5) } returns listOf(scoredArticle)
        every { articleSummarizer.summarize(listOf(scoredArticle), podcast, filterModelDef) } returns listOf(summarizedArticle)
        every { articleRepository.findRelevantUnprocessedBySourceIds(listOf("s1"), 5) } returns listOf(summarizedArticle)
        every { briefingComposer.compose(listOf(summarizedArticle), podcast, composeModelDef) } returns "Today in tech..."

        val result = pipeline.run(podcast)

        assertNotNull(result)
        assertEquals("Today in tech...", result!!.script)
        assertEquals("anthropic/claude-haiku-4.5", result.filterModel)
        assertEquals("anthropic/claude-sonnet-4", result.composeModel)

        verify { articleRepository.save(summarizedArticle.copy(isProcessed = true)) }
    }

    @Test
    fun `marks articles as processed with summaries preserved`() {
        val source = Source(id = "s1", podcastId = "p1", type = "rss", url = "https://example.com/feed")
        val article = Article(
            id = 1, sourceId = "s1", title = "AI News", body = "Article body",
            url = "https://example.com/ai", contentHash = "hash1",
            relevanceScore = 8, summary = "AI is advancing rapidly."
        )

        every { sourceRepository.findByPodcastId("p1") } returns listOf(source)
        every { modelResolver.resolve(podcast, "filter") } returns filterModelDef
        every { modelResolver.resolve(podcast, "compose") } returns composeModelDef
        every { articleRepository.findUnscoredBySourceIds(listOf("s1")) } returns emptyList()
        every { articleRepository.findRelevantUnsummarizedBySourceIds(listOf("s1"), 5) } returns emptyList()
        every { articleRepository.findRelevantUnprocessedBySourceIds(listOf("s1"), 5) } returns listOf(article)
        every { briefingComposer.compose(listOf(article), podcast, composeModelDef) } returns "Script text"

        val result = pipeline.run(podcast)

        assertNotNull(result)

        verify {
            articleRepository.save(match {
                it.isProcessed && it.summary == "AI is advancing rapidly." && it.relevanceScore == 8
            })
        }
    }

    @Test
    fun `pipeline resumes from summarization when scoring already done`() {
        val source = Source(id = "s1", podcastId = "p1", type = "rss", url = "https://example.com/feed")
        val scoredArticle = Article(
            id = 1, sourceId = "s1", title = "AI News", body = "Long article body ".repeat(50),
            url = "https://example.com/ai", contentHash = "hash1", relevanceScore = 7
        )

        every { sourceRepository.findByPodcastId("p1") } returns listOf(source)
        every { modelResolver.resolve(podcast, "filter") } returns filterModelDef
        every { modelResolver.resolve(podcast, "compose") } returns composeModelDef
        every { articleRepository.findUnscoredBySourceIds(listOf("s1")) } returns emptyList()
        every { articleRepository.findRelevantUnsummarizedBySourceIds(listOf("s1"), 5) } returns listOf(scoredArticle)
        every { articleSummarizer.summarize(listOf(scoredArticle), podcast, filterModelDef) } returns listOf(scoredArticle.copy(summary = "Summary."))
        every { articleRepository.findRelevantUnprocessedBySourceIds(listOf("s1"), 5) } returns listOf(scoredArticle.copy(summary = "Summary."))
        every { briefingComposer.compose(any(), podcast, composeModelDef) } returns "Briefing script"

        val result = pipeline.run(podcast)

        assertNotNull(result)
        // Scorer should not be called since no unscored articles
        verify(exactly = 0) { relevanceScorer.score(any(), any(), any()) }
        verify { articleSummarizer.summarize(listOf(scoredArticle), podcast, filterModelDef) }
    }
}

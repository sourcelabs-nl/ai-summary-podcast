package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.BriefingProperties
import com.aisummarypodcast.config.EncryptionProperties
import com.aisummarypodcast.config.EpisodesProperties
import com.aisummarypodcast.config.FeedProperties
import com.aisummarypodcast.config.LlmProperties
import com.aisummarypodcast.config.ModelDefinition
import com.aisummarypodcast.config.SourceProperties
import com.aisummarypodcast.source.SourceAggregator
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.Post
import com.aisummarypodcast.store.PostRepository
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

    private val articleScoreSummarizer = mockk<ArticleScoreSummarizer>()
    private val briefingComposer = mockk<BriefingComposer>()
    private val modelResolver = mockk<ModelResolver>()
    private val articleRepository = mockk<ArticleRepository> {
        every { save(any()) } answers { firstArg() }
    }
    private val sourceRepository = mockk<SourceRepository>()
    private val postRepository = mockk<PostRepository>()
    private val sourceAggregator = mockk<SourceAggregator>()

    private val appProperties = AppProperties(
        llm = LlmProperties(),
        briefing = BriefingProperties(),
        episodes = EpisodesProperties(),
        feed = FeedProperties(),
        encryption = EncryptionProperties(masterKey = "test-key"),
        source = SourceProperties(maxArticleAgeDays = 7)
    )

    private val filterModelDef = ModelDefinition(provider = "openrouter", model = "anthropic/claude-haiku-4.5")
    private val composeModelDef = ModelDefinition(provider = "openrouter", model = "anthropic/claude-sonnet-4")

    private val pipeline = LlmPipeline(
        articleScoreSummarizer, briefingComposer, modelResolver, articleRepository,
        sourceRepository, postRepository, sourceAggregator, appProperties
    )

    private val podcast = Podcast(id = "p1", userId = "u1", name = "Tech Daily", topic = "tech", relevanceThreshold = 5)
    private val source = Source(id = "s1", podcastId = "p1", type = "rss", url = "https://example.com/feed")

    @Test
    fun `returns null when podcast has no sources`() {
        every { sourceRepository.findByPodcastId("p1") } returns emptyList()

        val result = pipeline.run(podcast)

        assertNull(result)
    }

    @Test
    fun `returns null when no relevant unprocessed articles exist`() {
        every { sourceRepository.findByPodcastId("p1") } returns listOf(source)
        every { modelResolver.resolve(podcast, "filter") } returns filterModelDef
        every { modelResolver.resolve(podcast, "compose") } returns composeModelDef
        every { postRepository.findUnlinkedBySourceIds(listOf("s1"), any()) } returns emptyList()
        every { articleRepository.findUnscoredBySourceIds(listOf("s1")) } returns emptyList()
        every { articleRepository.findRelevantUnprocessedBySourceIds(listOf("s1"), 5) } returns emptyList()

        val result = pipeline.run(podcast)

        assertNull(result)
    }

    @Test
    fun `aggregates posts then scores and composes`() {
        val unlinkedPost = Post(
            id = 1, sourceId = "s1", title = "AI News", body = "Post body",
            url = "https://example.com/ai", contentHash = "hash1", createdAt = "2026-02-16T10:00:00Z"
        )
        val createdArticle = Article(
            id = 1, sourceId = "s1", title = "AI News", body = "Post body",
            url = "https://example.com/ai", contentHash = "arthash1"
        )
        val scoredArticle = createdArticle.copy(relevanceScore = 8, summary = "AI is advancing.")
        val compositionResult = CompositionResult("Today in tech...", TokenUsage(1000, 500))

        every { sourceRepository.findByPodcastId("p1") } returns listOf(source)
        every { modelResolver.resolve(podcast, "filter") } returns filterModelDef
        every { modelResolver.resolve(podcast, "compose") } returns composeModelDef
        every { postRepository.findUnlinkedBySourceIds(listOf("s1"), any()) } returns listOf(unlinkedPost)
        every { sourceAggregator.aggregateAndPersist(listOf(unlinkedPost), source) } returns listOf(createdArticle)
        every { articleRepository.findUnscoredBySourceIds(listOf("s1")) } returns listOf(createdArticle)
        every { articleScoreSummarizer.scoreSummarize(listOf(createdArticle), podcast, filterModelDef) } returns listOf(scoredArticle)
        every { articleRepository.findRelevantUnprocessedBySourceIds(listOf("s1"), 5) } returns listOf(scoredArticle)
        every { briefingComposer.compose(listOf(scoredArticle), podcast, composeModelDef) } returns compositionResult

        val result = pipeline.run(podcast)

        assertNotNull(result)
        assertEquals("Today in tech...", result!!.script)
        assertEquals("anthropic/claude-haiku-4.5", result.filterModel)
        assertEquals("anthropic/claude-sonnet-4", result.composeModel)

        verify { sourceAggregator.aggregateAndPersist(listOf(unlinkedPost), source) }
        verify { articleScoreSummarizer.scoreSummarize(listOf(createdArticle), podcast, filterModelDef) }
        verify { articleRepository.save(scoredArticle.copy(isProcessed = true)) }
    }

    @Test
    fun `marks articles as processed with summaries preserved`() {
        val article = Article(
            id = 1, sourceId = "s1", title = "AI News", body = "Article body",
            url = "https://example.com/ai", contentHash = "hash1",
            relevanceScore = 8, summary = "AI is advancing rapidly."
        )

        every { sourceRepository.findByPodcastId("p1") } returns listOf(source)
        every { modelResolver.resolve(podcast, "filter") } returns filterModelDef
        every { modelResolver.resolve(podcast, "compose") } returns composeModelDef
        every { postRepository.findUnlinkedBySourceIds(listOf("s1"), any()) } returns emptyList()
        every { articleRepository.findUnscoredBySourceIds(listOf("s1")) } returns emptyList()
        every { articleRepository.findRelevantUnprocessedBySourceIds(listOf("s1"), 5) } returns listOf(article)
        every { briefingComposer.compose(listOf(article), podcast, composeModelDef) } returns CompositionResult("Script text", TokenUsage(500, 200))

        val result = pipeline.run(podcast)

        assertNotNull(result)

        verify {
            articleRepository.save(match {
                it.isProcessed && it.summary == "AI is advancing rapidly." && it.relevanceScore == 8
            })
        }
    }

    @Test
    fun `skips aggregation when no unlinked posts exist`() {
        val scoredArticle = Article(
            id = 1, sourceId = "s1", title = "AI News", body = "Body",
            url = "https://example.com/ai", contentHash = "hash1", relevanceScore = 7,
            summary = "Summary."
        )

        every { sourceRepository.findByPodcastId("p1") } returns listOf(source)
        every { modelResolver.resolve(podcast, "filter") } returns filterModelDef
        every { modelResolver.resolve(podcast, "compose") } returns composeModelDef
        every { postRepository.findUnlinkedBySourceIds(listOf("s1"), any()) } returns emptyList()
        every { articleRepository.findUnscoredBySourceIds(listOf("s1")) } returns emptyList()
        every { articleRepository.findRelevantUnprocessedBySourceIds(listOf("s1"), 5) } returns listOf(scoredArticle)
        every { briefingComposer.compose(any(), podcast, composeModelDef) } returns CompositionResult("Briefing", TokenUsage(800, 300))

        val result = pipeline.run(podcast)

        assertNotNull(result)
        verify(exactly = 0) { sourceAggregator.aggregateAndPersist(any(), any()) }
        verify(exactly = 0) { articleScoreSummarizer.scoreSummarize(any(), any(), any()) }
    }
}

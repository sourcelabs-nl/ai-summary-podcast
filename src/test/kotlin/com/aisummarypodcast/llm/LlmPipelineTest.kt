package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.llm.PipelineStage
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
import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PodcastStyle
import com.aisummarypodcast.store.Post
import com.aisummarypodcast.store.PostRepository
import com.aisummarypodcast.store.Source
import com.aisummarypodcast.store.SourceRepository
import com.aisummarypodcast.store.SourceType
import com.aisummarypodcast.store.TtsProviderType
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
    private val dialogueComposer = mockk<DialogueComposer>()
    private val interviewComposer = mockk<InterviewComposer>()
    private val modelResolver = mockk<ModelResolver>()
    private val articleRepository = mockk<ArticleRepository> {
        every { save(any()) } answers { firstArg() }
    }
    private val sourceRepository = mockk<SourceRepository>()
    private val postRepository = mockk<PostRepository>()
    private val sourceAggregator = mockk<SourceAggregator>()
    private val episodeRepository = mockk<EpisodeRepository> {
        every { findMostRecentByPodcastId(any()) } returns null
    }

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
        articleScoreSummarizer, briefingComposer, dialogueComposer, interviewComposer, modelResolver, articleRepository,
        sourceRepository, postRepository, sourceAggregator, appProperties, episodeRepository
    )

    private val podcast = Podcast(id = "p1", userId = "u1", name = "Tech Daily", topic = "tech", relevanceThreshold = 5)
    private val source = Source(id = "s1", podcastId = "p1", type = SourceType.RSS, url = "https://example.com/feed")

    @Test
    fun `returns null when podcast has no sources`() {
        every { sourceRepository.findByPodcastId("p1") } returns emptyList()

        val result = pipeline.run(podcast)

        assertNull(result)
    }

    @Test
    fun `returns null when no relevant unprocessed articles exist`() {
        every { sourceRepository.findByPodcastId("p1") } returns listOf(source)
        every { modelResolver.resolve(podcast, PipelineStage.FILTER) } returns filterModelDef
        every { modelResolver.resolve(podcast, PipelineStage.COMPOSE) } returns composeModelDef
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
        every { modelResolver.resolve(podcast, PipelineStage.FILTER) } returns filterModelDef
        every { modelResolver.resolve(podcast, PipelineStage.COMPOSE) } returns composeModelDef
        every { postRepository.findUnlinkedBySourceIds(listOf("s1"), any()) } returns listOf(unlinkedPost)
        every { sourceAggregator.aggregateAndPersist(listOf(unlinkedPost), source) } returns listOf(createdArticle)
        every { articleRepository.findUnscoredBySourceIds(listOf("s1")) } returns listOf(createdArticle)
        every { articleScoreSummarizer.scoreSummarize(listOf(createdArticle), podcast, filterModelDef) } returns listOf(scoredArticle)
        every { articleRepository.findRelevantUnprocessedBySourceIds(listOf("s1"), 5) } returns listOf(scoredArticle)
        every { briefingComposer.compose(listOf(scoredArticle), podcast, composeModelDef, null) } returns compositionResult

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
        every { modelResolver.resolve(podcast, PipelineStage.FILTER) } returns filterModelDef
        every { modelResolver.resolve(podcast, PipelineStage.COMPOSE) } returns composeModelDef
        every { postRepository.findUnlinkedBySourceIds(listOf("s1"), any()) } returns emptyList()
        every { articleRepository.findUnscoredBySourceIds(listOf("s1")) } returns emptyList()
        every { articleRepository.findRelevantUnprocessedBySourceIds(listOf("s1"), 5) } returns listOf(article)
        every { briefingComposer.compose(listOf(article), podcast, composeModelDef, null) } returns CompositionResult("Script text", TokenUsage(500, 200))

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
        every { modelResolver.resolve(podcast, PipelineStage.FILTER) } returns filterModelDef
        every { modelResolver.resolve(podcast, PipelineStage.COMPOSE) } returns composeModelDef
        every { postRepository.findUnlinkedBySourceIds(listOf("s1"), any()) } returns emptyList()
        every { articleRepository.findUnscoredBySourceIds(listOf("s1")) } returns emptyList()
        every { articleRepository.findRelevantUnprocessedBySourceIds(listOf("s1"), 5) } returns listOf(scoredArticle)
        every { briefingComposer.compose(any(), any(), any(), any()) } returns CompositionResult("Briefing", TokenUsage(800, 300))

        val result = pipeline.run(podcast)

        assertNotNull(result)
        verify(exactly = 0) { sourceAggregator.aggregateAndPersist(any(), any()) }
        verify(exactly = 0) { articleScoreSummarizer.scoreSummarize(any(), any(), any()) }
    }

    @Test
    fun `per-podcast maxArticleAgeDays override is used for cutoff`() {
        // Global max is 7 days. Podcast overrides to 30 days.
        // We have an unlinked post from 15 days ago — should be included with the 30-day override.
        val podcastWith30Days = podcast.copy(maxArticleAgeDays = 30)

        every { sourceRepository.findByPodcastId("p1") } returns listOf(source)
        every { modelResolver.resolve(podcastWith30Days, PipelineStage.FILTER) } returns filterModelDef
        every { modelResolver.resolve(podcastWith30Days, PipelineStage.COMPOSE) } returns composeModelDef
        // The cutoff string is dynamic, so use any() matcher
        every { postRepository.findUnlinkedBySourceIds(listOf("s1"), any()) } returns emptyList()
        every { articleRepository.findUnscoredBySourceIds(listOf("s1")) } returns emptyList()
        every { articleRepository.findRelevantUnprocessedBySourceIds(listOf("s1"), 5) } returns emptyList()

        pipeline.run(podcastWith30Days)

        // Verify the cutoff passed to findUnlinkedBySourceIds is ~30 days ago, not 7
        verify {
            postRepository.findUnlinkedBySourceIds(listOf("s1"), match { cutoff ->
                // The cutoff should be roughly 30 days ago (within a minute tolerance)
                val cutoffInstant = java.time.Instant.parse(cutoff)
                val expected = java.time.Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS)
                val diff = kotlin.math.abs(cutoffInstant.epochSecond - expected.epochSecond)
                diff < 60 // within 60 seconds
            })
        }
    }

    // --- Cost gate tests ---

    private val pricedFilterModel = ModelDefinition(
        provider = "openrouter", model = "gpt-4o-mini",
        inputCostPerMtok = 0.15, outputCostPerMtok = 0.60
    )

    private val pricedComposeModel = ModelDefinition(
        provider = "openrouter", model = "claude-sonnet",
        inputCostPerMtok = 3.00, outputCostPerMtok = 15.00
    )

    // Helper: creates an article with a body of the given size
    private fun articleWithBody(bodySize: Int) = Article(
        id = null, sourceId = "s1", title = "Test", body = "x".repeat(bodySize),
        url = "http://test.com", contentHash = "hash-$bodySize"
    )

    private fun setupCostGateTest(
        podcast: Podcast,
        unscoredArticles: List<Article>,
        filterModel: ModelDefinition = pricedFilterModel,
        composeModel: ModelDefinition = pricedComposeModel
    ) {
        every { sourceRepository.findByPodcastId(podcast.id) } returns listOf(source)
        every { modelResolver.resolve(podcast, PipelineStage.FILTER) } returns filterModel
        every { modelResolver.resolve(podcast, PipelineStage.COMPOSE) } returns composeModel
        every { postRepository.findUnlinkedBySourceIds(listOf("s1"), any()) } returns emptyList()
        every { articleRepository.findUnscoredBySourceIds(listOf("s1")) } returns unscoredArticles
    }

    @Test
    fun `cost gate - below threshold proceeds with pipeline`() {
        // Small articles → low cost, well under 200¢ default threshold
        val articles = listOf(articleWithBody(2000))
        val scoredArticle = articles[0].copy(relevanceScore = 8, summary = "Summary")
        val podcastWithPricing = podcast.copy()

        setupCostGateTest(podcastWithPricing, articles)
        every { articleScoreSummarizer.scoreSummarize(articles, podcastWithPricing, pricedFilterModel) } returns listOf(scoredArticle)
        every { articleRepository.findRelevantUnprocessedBySourceIds(listOf("s1"), 5) } returns listOf(scoredArticle)
        every { briefingComposer.compose(listOf(scoredArticle), podcastWithPricing, pricedComposeModel, null) } returns CompositionResult("Script", TokenUsage(500, 200))

        val result = pipeline.run(podcastWithPricing)

        assertNotNull(result)
        verify { briefingComposer.compose(any(), any(), any(), any()) }
    }

    @Test
    fun `cost gate - above threshold skips pipeline`() {
        // Use a very low threshold (1¢) so even small articles trigger the gate
        val lowThresholdProps = AppProperties(
            llm = LlmProperties(maxCostCents = 1),
            briefing = BriefingProperties(),
            episodes = EpisodesProperties(),
            feed = FeedProperties(),
            encryption = EncryptionProperties(masterKey = "test-key"),
            source = SourceProperties(maxArticleAgeDays = 7)
        )
        val pipelineWithLowThreshold = LlmPipeline(
            articleScoreSummarizer, briefingComposer, dialogueComposer, interviewComposer, modelResolver, articleRepository,
            sourceRepository, postRepository, sourceAggregator, lowThresholdProps, episodeRepository
        )

        // 100 articles with 10000 chars each → estimated cost will exceed 1¢
        val articles = (1..100).map { articleWithBody(10000) }

        every { sourceRepository.findByPodcastId("p1") } returns listOf(source)
        every { modelResolver.resolve(podcast, PipelineStage.FILTER) } returns pricedFilterModel
        every { modelResolver.resolve(podcast, PipelineStage.COMPOSE) } returns pricedComposeModel
        every { postRepository.findUnlinkedBySourceIds(listOf("s1"), any()) } returns emptyList()
        every { articleRepository.findUnscoredBySourceIds(listOf("s1")) } returns articles

        val result = pipelineWithLowThreshold.run(podcast)

        assertNull(result)
        verify(exactly = 0) { articleScoreSummarizer.scoreSummarize(any(), any(), any()) }
        verify(exactly = 0) { briefingComposer.compose(any(), any(), any(), any()) }
    }

    @Test
    fun `cost gate - equal threshold proceeds`() {
        // We need to find a cost that exactly equals the threshold.
        // Use a custom threshold that matches the estimated cost.
        // For 1 article with 4 chars body: scoring input = 1 token, output = 200 tokens
        // Scoring cost = (1 * 0.15 + 200 * 0.60) / 1_000_000 * 100 ≈ 0.012 cents → 0
        // Composition: input = 200 tokens, output = 1500 * 1.3 = 1950 tokens
        // Composition cost = (200 * 3.00 + 1950 * 15.00) / 1_000_000 * 100 ≈ 2.985 → 3
        // Total: 0 + 3 = 3 cents
        val thresholdProps = AppProperties(
            llm = LlmProperties(maxCostCents = 3),
            briefing = BriefingProperties(),
            episodes = EpisodesProperties(),
            feed = FeedProperties(),
            encryption = EncryptionProperties(masterKey = "test-key"),
            source = SourceProperties(maxArticleAgeDays = 7)
        )
        val pipelineExact = LlmPipeline(
            articleScoreSummarizer, briefingComposer, dialogueComposer, interviewComposer, modelResolver, articleRepository,
            sourceRepository, postRepository, sourceAggregator, thresholdProps, episodeRepository
        )

        val articles = listOf(articleWithBody(4))
        val scoredArticle = articles[0].copy(relevanceScore = 8, summary = "Summary")

        every { sourceRepository.findByPodcastId("p1") } returns listOf(source)
        every { modelResolver.resolve(podcast, PipelineStage.FILTER) } returns pricedFilterModel
        every { modelResolver.resolve(podcast, PipelineStage.COMPOSE) } returns pricedComposeModel
        every { postRepository.findUnlinkedBySourceIds(listOf("s1"), any()) } returns emptyList()
        every { articleRepository.findUnscoredBySourceIds(listOf("s1")) } returns articles
        every { articleScoreSummarizer.scoreSummarize(articles, podcast, pricedFilterModel) } returns listOf(scoredArticle)
        every { articleRepository.findRelevantUnprocessedBySourceIds(listOf("s1"), 5) } returns listOf(scoredArticle)
        every { briefingComposer.compose(listOf(scoredArticle), podcast, pricedComposeModel, null) } returns CompositionResult("Script", TokenUsage(500, 200))

        val result = pipelineExact.run(podcast)

        assertNotNull(result)
        verify { briefingComposer.compose(any(), any(), any(), any()) }
    }

    @Test
    fun `cost gate - null pricing bypasses gate`() {
        // Models without pricing → cost estimate is null → gate bypassed
        val articles = listOf(articleWithBody(2000))
        val scoredArticle = articles[0].copy(relevanceScore = 8, summary = "Summary")

        setupCostGateTest(podcast, articles, filterModelDef, composeModelDef)
        every { articleScoreSummarizer.scoreSummarize(articles, podcast, filterModelDef) } returns listOf(scoredArticle)
        every { articleRepository.findRelevantUnprocessedBySourceIds(listOf("s1"), 5) } returns listOf(scoredArticle)
        every { briefingComposer.compose(listOf(scoredArticle), podcast, composeModelDef) } returns CompositionResult("Script", TokenUsage(500, 200))

        val result = pipeline.run(podcast)

        assertNotNull(result)
        verify { briefingComposer.compose(any(), any(), any(), any()) }
    }

    @Test
    fun `cost gate - per-podcast override respected`() {
        // Global threshold is 200 (default). Podcast overrides to 500.
        // With 100 articles of 10000 chars, the cost will exceed 200 but not 500.
        // Let's verify: scoring input = 100 * (10000/4) = 250000 tokens, output = 100 * 200 = 20000 tokens
        // Scoring cost = (250000 * 0.15 + 20000 * 0.60) / 1_000_000 * 100 = 4.95 cents → 5
        // Composition: input = 100 * 200 = 20000, output = 1950
        // Composition cost = (20000 * 3.00 + 1950 * 15.00) / 1_000_000 * 100 = 8.925 cents → 9
        // Total = 5 + 9 = 14 cents → well under 500, also under 200
        // Need more articles to exceed 200 but not 500... let me use larger bodies.
        // Actually 14 cents for 100 articles is very low. The default 200¢ won't trigger here.
        // Let me use a low global threshold with a higher per-podcast override.
        val lowGlobalProps = AppProperties(
            llm = LlmProperties(maxCostCents = 1),
            briefing = BriefingProperties(),
            episodes = EpisodesProperties(),
            feed = FeedProperties(),
            encryption = EncryptionProperties(masterKey = "test-key"),
            source = SourceProperties(maxArticleAgeDays = 7)
        )
        val pipelineWithLowGlobal = LlmPipeline(
            articleScoreSummarizer, briefingComposer, dialogueComposer, interviewComposer, modelResolver, articleRepository,
            sourceRepository, postRepository, sourceAggregator, lowGlobalProps, episodeRepository
        )

        // Per-podcast override to 500 → the estimate (14¢) is under 500 → passes
        val podcastWithOverride = podcast.copy(maxLlmCostCents = 500)
        val articles = (1..100).map { articleWithBody(10000) }
        val scoredArticles = articles.mapIndexed { i, a -> a.copy(relevanceScore = 8, summary = "Summary $i") }

        every { sourceRepository.findByPodcastId("p1") } returns listOf(source)
        every { modelResolver.resolve(podcastWithOverride, PipelineStage.FILTER) } returns pricedFilterModel
        every { modelResolver.resolve(podcastWithOverride, PipelineStage.COMPOSE) } returns pricedComposeModel
        every { postRepository.findUnlinkedBySourceIds(listOf("s1"), any()) } returns emptyList()
        every { articleRepository.findUnscoredBySourceIds(listOf("s1")) } returns articles
        every { articleScoreSummarizer.scoreSummarize(articles, podcastWithOverride, pricedFilterModel) } returns scoredArticles
        every { articleRepository.findRelevantUnprocessedBySourceIds(listOf("s1"), 5) } returns scoredArticles
        every { briefingComposer.compose(scoredArticles, podcastWithOverride, pricedComposeModel, null) } returns CompositionResult("Script", TokenUsage(500, 200))

        val result = pipelineWithLowGlobal.run(podcastWithOverride)

        // Without the per-podcast override (global = 1¢), this would be blocked
        // With override = 500¢, it passes
        assertNotNull(result)
        verify { briefingComposer.compose(any(), any(), any(), any()) }
    }

    // --- Composer selection tests ---

    @Test
    fun `uses dialogueComposer for dialogue style podcast`() {
        val dialoguePodcast = podcast.copy(style = PodcastStyle.DIALOGUE, ttsProvider = TtsProviderType.ELEVENLABS, ttsVoices = mapOf("host" to "v1", "cohost" to "v2"))
        val article = Article(
            id = 1, sourceId = "s1", title = "AI News", body = "Body",
            url = "https://example.com/ai", contentHash = "hash1", relevanceScore = 8, summary = "Summary."
        )
        val compositionResult = CompositionResult("<host>Hello!</host><cohost>Hi!</cohost>", TokenUsage(500, 200))

        every { sourceRepository.findByPodcastId("p1") } returns listOf(source)
        every { modelResolver.resolve(dialoguePodcast, PipelineStage.FILTER) } returns filterModelDef
        every { modelResolver.resolve(dialoguePodcast, PipelineStage.COMPOSE) } returns composeModelDef
        every { postRepository.findUnlinkedBySourceIds(listOf("s1"), any()) } returns emptyList()
        every { articleRepository.findUnscoredBySourceIds(listOf("s1")) } returns emptyList()
        every { articleRepository.findRelevantUnprocessedBySourceIds(listOf("s1"), 5) } returns listOf(article)
        every { dialogueComposer.compose(listOf(article), dialoguePodcast, composeModelDef, null) } returns compositionResult

        val result = pipeline.run(dialoguePodcast)

        assertNotNull(result)
        verify { dialogueComposer.compose(any(), any(), any(), any()) }
        verify(exactly = 0) { briefingComposer.compose(any(), any(), any(), any()) }
    }

    @Test
    fun `uses interviewComposer for interview style podcast`() {
        val interviewPodcast = podcast.copy(style = PodcastStyle.INTERVIEW, ttsProvider = TtsProviderType.ELEVENLABS, ttsVoices = mapOf("interviewer" to "v1", "expert" to "v2"))
        val article = Article(
            id = 1, sourceId = "s1", title = "AI News", body = "Body",
            url = "https://example.com/ai", contentHash = "hash1", relevanceScore = 8, summary = "Summary."
        )
        val compositionResult = CompositionResult("<interviewer>Question?</interviewer><expert>Answer.</expert>", TokenUsage(500, 200))

        every { sourceRepository.findByPodcastId("p1") } returns listOf(source)
        every { modelResolver.resolve(interviewPodcast, PipelineStage.FILTER) } returns filterModelDef
        every { modelResolver.resolve(interviewPodcast, PipelineStage.COMPOSE) } returns composeModelDef
        every { postRepository.findUnlinkedBySourceIds(listOf("s1"), any()) } returns emptyList()
        every { articleRepository.findUnscoredBySourceIds(listOf("s1")) } returns emptyList()
        every { articleRepository.findRelevantUnprocessedBySourceIds(listOf("s1"), 5) } returns listOf(article)
        every { interviewComposer.compose(listOf(article), interviewPodcast, composeModelDef, null) } returns compositionResult

        val result = pipeline.run(interviewPodcast)

        assertNotNull(result)
        verify { interviewComposer.compose(any(), any(), any(), any()) }
        verify(exactly = 0) { dialogueComposer.compose(any(), any(), any(), any()) }
        verify(exactly = 0) { briefingComposer.compose(any(), any(), any(), any()) }
    }

    @Test
    fun `uses briefingComposer for non-dialogue style podcast`() {
        val newsBriefingPodcast = podcast.copy(style = PodcastStyle.NEWS_BRIEFING)
        val article = Article(
            id = 1, sourceId = "s1", title = "AI News", body = "Body",
            url = "https://example.com/ai", contentHash = "hash1", relevanceScore = 8, summary = "Summary."
        )
        val compositionResult = CompositionResult("Today in tech...", TokenUsage(500, 200))

        every { sourceRepository.findByPodcastId("p1") } returns listOf(source)
        every { modelResolver.resolve(newsBriefingPodcast, PipelineStage.FILTER) } returns filterModelDef
        every { modelResolver.resolve(newsBriefingPodcast, PipelineStage.COMPOSE) } returns composeModelDef
        every { postRepository.findUnlinkedBySourceIds(listOf("s1"), any()) } returns emptyList()
        every { articleRepository.findUnscoredBySourceIds(listOf("s1")) } returns emptyList()
        every { articleRepository.findRelevantUnprocessedBySourceIds(listOf("s1"), 5) } returns listOf(article)
        every { briefingComposer.compose(listOf(article), newsBriefingPodcast, composeModelDef, null) } returns compositionResult

        val result = pipeline.run(newsBriefingPodcast)

        assertNotNull(result)
        verify { briefingComposer.compose(any(), any(), any(), any()) }
        verify(exactly = 0) { dialogueComposer.compose(any(), any(), any(), any()) }
    }

    // --- Episode continuity context tests ---

    @Test
    fun `passes previous episode recap to briefing composer`() {
        val article = Article(
            id = 1, sourceId = "s1", title = "AI News", body = "Body",
            url = "https://example.com/ai", contentHash = "hash1", relevanceScore = 8, summary = "Summary."
        )
        val previousEpisode = Episode(
            id = 10, podcastId = "p1", generatedAt = "2026-02-18T10:00:00Z",
            scriptText = "Previous script", recap = "AI chip shortages. EU regulations."
        )

        every { sourceRepository.findByPodcastId("p1") } returns listOf(source)
        every { modelResolver.resolve(podcast, PipelineStage.FILTER) } returns filterModelDef
        every { modelResolver.resolve(podcast, PipelineStage.COMPOSE) } returns composeModelDef
        every { postRepository.findUnlinkedBySourceIds(listOf("s1"), any()) } returns emptyList()
        every { articleRepository.findUnscoredBySourceIds(listOf("s1")) } returns emptyList()
        every { articleRepository.findRelevantUnprocessedBySourceIds(listOf("s1"), 5) } returns listOf(article)
        every { episodeRepository.findMostRecentByPodcastId("p1") } returns previousEpisode
        every { briefingComposer.compose(listOf(article), podcast, composeModelDef, "AI chip shortages. EU regulations.") } returns CompositionResult("Script with recap", TokenUsage(500, 200))

        val result = pipeline.run(podcast)

        assertNotNull(result)
        verify { briefingComposer.compose(listOf(article), podcast, composeModelDef, "AI chip shortages. EU regulations.") }
    }

    @Test
    fun `passes null recap when no previous episode exists`() {
        val article = Article(
            id = 1, sourceId = "s1", title = "AI News", body = "Body",
            url = "https://example.com/ai", contentHash = "hash1", relevanceScore = 8, summary = "Summary."
        )

        every { sourceRepository.findByPodcastId("p1") } returns listOf(source)
        every { modelResolver.resolve(podcast, PipelineStage.FILTER) } returns filterModelDef
        every { modelResolver.resolve(podcast, PipelineStage.COMPOSE) } returns composeModelDef
        every { postRepository.findUnlinkedBySourceIds(listOf("s1"), any()) } returns emptyList()
        every { articleRepository.findUnscoredBySourceIds(listOf("s1")) } returns emptyList()
        every { articleRepository.findRelevantUnprocessedBySourceIds(listOf("s1"), 5) } returns listOf(article)
        every { episodeRepository.findMostRecentByPodcastId("p1") } returns null
        every { briefingComposer.compose(listOf(article), podcast, composeModelDef, null) } returns CompositionResult("Script without recap", TokenUsage(500, 200))

        val result = pipeline.run(podcast)

        assertNotNull(result)
        verify { briefingComposer.compose(listOf(article), podcast, composeModelDef, null) }
    }

    @Test
    fun `passes null recap when previous episode has no recap`() {
        val article = Article(
            id = 1, sourceId = "s1", title = "AI News", body = "Body",
            url = "https://example.com/ai", contentHash = "hash1", relevanceScore = 8, summary = "Summary."
        )
        val previousEpisode = Episode(
            id = 10, podcastId = "p1", generatedAt = "2026-02-18T10:00:00Z",
            scriptText = "Old script", recap = null
        )

        every { sourceRepository.findByPodcastId("p1") } returns listOf(source)
        every { modelResolver.resolve(podcast, PipelineStage.FILTER) } returns filterModelDef
        every { modelResolver.resolve(podcast, PipelineStage.COMPOSE) } returns composeModelDef
        every { postRepository.findUnlinkedBySourceIds(listOf("s1"), any()) } returns emptyList()
        every { articleRepository.findUnscoredBySourceIds(listOf("s1")) } returns emptyList()
        every { articleRepository.findRelevantUnprocessedBySourceIds(listOf("s1"), 5) } returns listOf(article)
        every { episodeRepository.findMostRecentByPodcastId("p1") } returns previousEpisode
        every { briefingComposer.compose(listOf(article), podcast, composeModelDef, null) } returns CompositionResult("Script without recap", TokenUsage(500, 200))

        val result = pipeline.run(podcast)

        assertNotNull(result)
        verify { briefingComposer.compose(listOf(article), podcast, composeModelDef, null) }
    }
}

package com.aisummarypodcast.podcast

import com.aisummarypodcast.llm.EpisodeRecapGenerator
import com.aisummarypodcast.llm.ModelResolver
import com.aisummarypodcast.llm.PipelineResult
import com.aisummarypodcast.llm.PipelineStage
import com.aisummarypodcast.llm.RecapResult
import com.aisummarypodcast.llm.TokenUsage
import com.aisummarypodcast.config.ModelDefinition
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.EpisodeArticle
import com.aisummarypodcast.store.EpisodeArticleRepository
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.store.EpisodeStatus
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PodcastRepository
import com.aisummarypodcast.store.PostArticleRepository
import com.aisummarypodcast.tts.TtsPipeline
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class EpisodeServiceTest {

    private val episodeRepository = mockk<EpisodeRepository>()
    private val podcastRepository = mockk<PodcastRepository>()
    private val ttsPipeline = mockk<TtsPipeline>()
    private val episodeArticleRepository = mockk<EpisodeArticleRepository> {
        every { save(any()) } answers { firstArg() }
    }
    private val articleRepository = mockk<ArticleRepository>()
    private val episodeRecapGenerator = mockk<EpisodeRecapGenerator>()
    private val modelResolver = mockk<ModelResolver>()
    private val postArticleRepository = mockk<PostArticleRepository>()

    private val filterModelDef = ModelDefinition(provider = "openrouter", model = "anthropic/claude-haiku-4.5")

    private val episodeService = EpisodeService(
        episodeRepository, podcastRepository, ttsPipeline,
        episodeArticleRepository, articleRepository, episodeRecapGenerator, modelResolver,
        postArticleRepository
    )

    private val podcast = Podcast(id = "p1", userId = "u1", name = "Test", topic = "tech")

    private fun setupRecapMocks(podcast: Podcast) {
        every { modelResolver.resolve(podcast, PipelineStage.FILTER) } returns filterModelDef
        every { episodeRecapGenerator.generate(any(), podcast, filterModelDef) } returns RecapResult(
            recap = "Recap text.", usage = TokenUsage(800, 60)
        )
    }

    // --- createEpisodeFromPipelineResult tests ---

    @Test
    fun `creates PENDING_REVIEW episode when requireReview is true`() {
        val reviewPodcast = podcast.copy(requireReview = true)
        val result = PipelineResult(
            script = "Script", filterModel = "filter", composeModel = "compose",
            processedArticleIds = listOf(10L, 20L)
        )
        every { episodeRepository.save(any()) } answers { firstArg<Episode>().copy(id = 5) }
        every { podcastRepository.save(any()) } answers { firstArg() }
        setupRecapMocks(reviewPodcast)

        val episode = episodeService.createEpisodeFromPipelineResult(reviewPodcast, result)

        assertEquals(EpisodeStatus.PENDING_REVIEW, episode.status)
        verify { episodeArticleRepository.save(match { it.episodeId == 5L && it.articleId == 10L }) }
        verify { episodeArticleRepository.save(match { it.episodeId == 5L && it.articleId == 20L }) }
    }

    @Test
    fun `creates GENERATED episode with TTS when requireReview is false`() {
        val ttsEpisode = Episode(
            id = 5, podcastId = "p1", generatedAt = "now",
            scriptText = "Script", audioFilePath = "/audio.mp3", durationSeconds = 60
        )
        val result = PipelineResult(
            script = "Script", filterModel = "filter", composeModel = "compose",
            processedArticleIds = listOf(10L)
        )
        every { ttsPipeline.generate("Script", podcast) } returns ttsEpisode
        every { episodeRepository.save(any()) } answers { firstArg<Episode>().copy(id = 5) }
        every { podcastRepository.save(any()) } answers { firstArg() }
        setupRecapMocks(podcast)

        val episode = episodeService.createEpisodeFromPipelineResult(podcast, result)

        verify { ttsPipeline.generate("Script", podcast) }
        verify { episodeArticleRepository.save(match { it.episodeId == 5L && it.articleId == 10L }) }
    }

    @Test
    fun `saves episode-article links for all processed articles`() {
        val reviewPodcast = podcast.copy(requireReview = true)
        val result = PipelineResult(
            script = "Script", filterModel = "filter", composeModel = "compose",
            processedArticleIds = listOf(10L, 20L, 30L)
        )
        every { episodeRepository.save(any()) } answers { firstArg<Episode>().copy(id = 7) }
        every { podcastRepository.save(any()) } answers { firstArg() }
        setupRecapMocks(reviewPodcast)

        episodeService.createEpisodeFromPipelineResult(reviewPodcast, result)

        verify(exactly = 3) { episodeArticleRepository.save(any()) }
        verify { episodeArticleRepository.save(match { it.episodeId == 7L && it.articleId == 10L }) }
        verify { episodeArticleRepository.save(match { it.episodeId == 7L && it.articleId == 20L }) }
        verify { episodeArticleRepository.save(match { it.episodeId == 7L && it.articleId == 30L }) }
    }

    @Test
    fun `generates and stores recap`() {
        val reviewPodcast = podcast.copy(requireReview = true)
        val result = PipelineResult(
            script = "Script", filterModel = "filter", composeModel = "compose",
            llmInputTokens = 1000, llmOutputTokens = 500
        )
        every { episodeRepository.save(any()) } answers { firstArg<Episode>().copy(id = 5) }
        every { podcastRepository.save(any()) } answers { firstArg() }
        setupRecapMocks(reviewPodcast)

        episodeService.createEpisodeFromPipelineResult(reviewPodcast, result)

        verify { episodeRepository.save(match { it.recap == "Recap text." }) }
    }

    @Test
    fun `recap failure does not block episode creation`() {
        val reviewPodcast = podcast.copy(requireReview = true)
        val result = PipelineResult(script = "Script", filterModel = "filter", composeModel = "compose")
        every { episodeRepository.save(any()) } answers { firstArg<Episode>().copy(id = 5) }
        every { podcastRepository.save(any()) } answers { firstArg() }
        every { modelResolver.resolve(reviewPodcast, PipelineStage.FILTER) } returns filterModelDef
        every { episodeRecapGenerator.generate(any(), reviewPodcast, filterModelDef) } throws RuntimeException("LLM error")

        episodeService.createEpisodeFromPipelineResult(reviewPodcast, result)

        verify { podcastRepository.save(match { it.lastGeneratedAt != null }) }
    }

    @Test
    fun `updates lastGeneratedAt on podcast`() {
        val reviewPodcast = podcast.copy(requireReview = true)
        val result = PipelineResult(script = "Script", filterModel = "filter", composeModel = "compose")
        every { episodeRepository.save(any()) } answers { firstArg<Episode>().copy(id = 5) }
        every { podcastRepository.save(any()) } answers { firstArg() }
        setupRecapMocks(reviewPodcast)

        episodeService.createEpisodeFromPipelineResult(reviewPodcast, result)

        verify { podcastRepository.save(match { it.lastGeneratedAt != null }) }
    }

    // --- generateAudioAsync tests ---

    private val approvedEpisode = Episode(
        id = 1L, podcastId = "p1", generatedAt = "2025-01-01T00:00:00Z",
        scriptText = "Test script", status = EpisodeStatus.APPROVED
    )

    @Test
    fun `generates audio and updates status to GENERATED on success`() {
        val generatedEpisode = approvedEpisode.copy(status = EpisodeStatus.GENERATED, audioFilePath = "/audio.mp3", durationSeconds = 120)
        every { episodeRepository.findById(1L) } returns Optional.of(approvedEpisode)
        every { podcastRepository.findById("p1") } returns Optional.of(podcast)
        every { ttsPipeline.generateForExistingEpisode(approvedEpisode, podcast) } returns generatedEpisode

        episodeService.generateAudioAsync(1L, "p1")

        verify { ttsPipeline.generateForExistingEpisode(approvedEpisode, podcast) }
    }

    @Test
    fun `updates status to FAILED on TTS error`() {
        every { episodeRepository.findById(1L) } returns Optional.of(approvedEpisode)
        every { podcastRepository.findById("p1") } returns Optional.of(podcast)
        every { ttsPipeline.generateForExistingEpisode(approvedEpisode, podcast) } throws RuntimeException("TTS failure")
        every { episodeRepository.save(any()) } answers { firstArg() }

        episodeService.generateAudioAsync(1L, "p1")

        verify { episodeRepository.save(match { it.status == EpisodeStatus.FAILED }) }
    }

    @Test
    fun `updates status to FAILED when podcast not found`() {
        every { episodeRepository.findById(1L) } returns Optional.of(approvedEpisode)
        every { podcastRepository.findById("p1") } returns Optional.empty()
        every { episodeRepository.save(any()) } answers { firstArg() }

        episodeService.generateAudioAsync(1L, "p1")

        verify { episodeRepository.save(match { it.status == EpisodeStatus.FAILED }) }
    }

    // --- discardAndResetArticles tests ---

    @Test
    fun `discardAndResetArticles resets non-aggregated articles`() {
        val episode = Episode(id = 1L, podcastId = "p1", generatedAt = "now", scriptText = "Script", status = EpisodeStatus.PENDING_REVIEW)
        val article1 = Article(id = 10L, sourceId = "src-1", title = "A1", body = "body", url = "https://example.com/1", contentHash = "h1", isProcessed = true)
        val article2 = Article(id = 20L, sourceId = "src-1", title = "A2", body = "body", url = "https://example.com/2", contentHash = "h2", isProcessed = true)
        val links = listOf(
            EpisodeArticle(id = 1L, episodeId = 1L, articleId = 10L),
            EpisodeArticle(id = 2L, episodeId = 1L, articleId = 20L)
        )

        every { episodeRepository.save(any()) } answers { firstArg() }
        every { episodeArticleRepository.findByEpisodeId(1L) } returns links
        every { articleRepository.findById(10L) } returns Optional.of(article1)
        every { articleRepository.findById(20L) } returns Optional.of(article2)
        every { articleRepository.save(any()) } answers { firstArg() }
        every { postArticleRepository.countByArticleId(10L) } returns 1L
        every { postArticleRepository.countByArticleId(20L) } returns 0L

        episodeService.discardAndResetArticles(episode)

        verify { episodeRepository.save(match { it.status == EpisodeStatus.DISCARDED }) }
        verify { articleRepository.save(match { it.id == 10L && !it.isProcessed }) }
        verify { articleRepository.save(match { it.id == 20L && !it.isProcessed }) }
        verify(exactly = 0) { postArticleRepository.deleteByArticleId(any()) }
        verify(exactly = 0) { articleRepository.deleteById(any<Long>()) }
    }

    @Test
    fun `discardAndResetArticles deletes aggregated articles and unlinks posts`() {
        val episode = Episode(id = 1L, podcastId = "p1", generatedAt = "now", scriptText = "Script", status = EpisodeStatus.PENDING_REVIEW)
        val aggregatedArticle = Article(id = 30L, sourceId = "src-1", title = "Posts from @user", body = "body", url = "https://nitter.net/user", contentHash = "h3", isProcessed = true)
        val links = listOf(EpisodeArticle(id = 1L, episodeId = 1L, articleId = 30L))

        every { episodeRepository.save(any()) } answers { firstArg() }
        every { episodeArticleRepository.findByEpisodeId(1L) } returns links
        every { articleRepository.findById(30L) } returns Optional.of(aggregatedArticle)
        every { postArticleRepository.countByArticleId(30L) } returns 5L
        justRun { postArticleRepository.deleteByArticleId(30L) }
        justRun { articleRepository.deleteById(30L) }

        episodeService.discardAndResetArticles(episode)

        verify { episodeRepository.save(match { it.status == EpisodeStatus.DISCARDED }) }
        verify { postArticleRepository.deleteByArticleId(30L) }
        verify { articleRepository.deleteById(30L) }
        verify(exactly = 0) { articleRepository.save(any()) }
    }

    @Test
    fun `discardAndResetArticles handles mixed article types`() {
        val episode = Episode(id = 1L, podcastId = "p1", generatedAt = "now", scriptText = "Script", status = EpisodeStatus.PENDING_REVIEW)
        val regularArticle = Article(id = 10L, sourceId = "src-1", title = "A1", body = "body", url = "https://example.com/1", contentHash = "h1", isProcessed = true)
        val aggregatedArticle = Article(id = 30L, sourceId = "src-2", title = "Posts from @user", body = "body", url = "https://nitter.net/user", contentHash = "h3", isProcessed = true)
        val links = listOf(
            EpisodeArticle(id = 1L, episodeId = 1L, articleId = 10L),
            EpisodeArticle(id = 2L, episodeId = 1L, articleId = 30L)
        )

        every { episodeRepository.save(any()) } answers { firstArg() }
        every { episodeArticleRepository.findByEpisodeId(1L) } returns links
        every { articleRepository.findById(10L) } returns Optional.of(regularArticle)
        every { articleRepository.findById(30L) } returns Optional.of(aggregatedArticle)
        every { articleRepository.save(any()) } answers { firstArg() }
        every { postArticleRepository.countByArticleId(10L) } returns 1L
        every { postArticleRepository.countByArticleId(30L) } returns 4L
        justRun { postArticleRepository.deleteByArticleId(30L) }
        justRun { articleRepository.deleteById(30L) }

        episodeService.discardAndResetArticles(episode)

        verify { episodeRepository.save(match { it.status == EpisodeStatus.DISCARDED }) }
        verify { articleRepository.save(match { it.id == 10L && !it.isProcessed }) }
        verify { postArticleRepository.deleteByArticleId(30L) }
        verify { articleRepository.deleteById(30L) }
    }

    @Test
    fun `discardAndResetArticles handles no linked articles`() {
        val episode = Episode(id = 1L, podcastId = "p1", generatedAt = "now", scriptText = "Script", status = EpisodeStatus.PENDING_REVIEW)

        every { episodeRepository.save(any()) } answers { firstArg() }
        every { episodeArticleRepository.findByEpisodeId(1L) } returns emptyList()

        episodeService.discardAndResetArticles(episode)

        verify { episodeRepository.save(match { it.status == EpisodeStatus.DISCARDED }) }
        verify(exactly = 0) { articleRepository.save(any()) }
    }

    // --- hasPendingOrApprovedEpisode tests ---

    @Test
    fun `hasPendingOrApprovedEpisode returns true when pending episodes exist`() {
        every { episodeRepository.findByPodcastIdAndStatusIn("p1", listOf("PENDING_REVIEW", "APPROVED")) } returns listOf(
            Episode(id = 1L, podcastId = "p1", generatedAt = "now", scriptText = "Script", status = EpisodeStatus.PENDING_REVIEW)
        )

        assertEquals(true, episodeService.hasPendingOrApprovedEpisode("p1"))
    }

    @Test
    fun `hasPendingOrApprovedEpisode returns false when no pending episodes`() {
        every { episodeRepository.findByPodcastIdAndStatusIn("p1", listOf("PENDING_REVIEW", "APPROVED")) } returns emptyList()

        assertEquals(false, episodeService.hasPendingOrApprovedEpisode("p1"))
    }
}

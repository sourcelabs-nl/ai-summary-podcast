package com.aisummarypodcast.podcast

import com.aisummarypodcast.llm.EpisodeRecapGenerator
import com.aisummarypodcast.llm.ModelResolver
import com.aisummarypodcast.llm.PipelineResult
import com.aisummarypodcast.llm.RecapResult
import com.aisummarypodcast.llm.TokenUsage
import com.aisummarypodcast.config.ModelDefinition
import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.EpisodeArticle
import com.aisummarypodcast.store.EpisodeArticleRepository
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PodcastRepository
import com.aisummarypodcast.tts.TtsPipeline
import io.mockk.every
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
    private val episodeRecapGenerator = mockk<EpisodeRecapGenerator>()
    private val modelResolver = mockk<ModelResolver>()

    private val filterModelDef = ModelDefinition(provider = "openrouter", model = "anthropic/claude-haiku-4.5")

    private val episodeService = EpisodeService(
        episodeRepository, podcastRepository, ttsPipeline,
        episodeArticleRepository, episodeRecapGenerator, modelResolver
    )

    private val podcast = Podcast(id = "p1", userId = "u1", name = "Test", topic = "tech")

    private fun setupRecapMocks(podcast: Podcast) {
        every { modelResolver.resolve(podcast, "filter") } returns filterModelDef
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

        assertEquals("PENDING_REVIEW", episode.status)
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
        every { modelResolver.resolve(reviewPodcast, "filter") } returns filterModelDef
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
        scriptText = "Test script", status = "APPROVED"
    )

    @Test
    fun `generates audio and updates status to GENERATED on success`() {
        val generatedEpisode = approvedEpisode.copy(status = "GENERATED", audioFilePath = "/audio.mp3", durationSeconds = 120)
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

        verify { episodeRepository.save(match { it.status == "FAILED" }) }
    }

    @Test
    fun `updates status to FAILED when podcast not found`() {
        every { episodeRepository.findById(1L) } returns Optional.of(approvedEpisode)
        every { podcastRepository.findById("p1") } returns Optional.empty()
        every { episodeRepository.save(any()) } answers { firstArg() }

        episodeService.generateAudioAsync(1L, "p1")

        verify { episodeRepository.save(match { it.status == "FAILED" }) }
    }
}

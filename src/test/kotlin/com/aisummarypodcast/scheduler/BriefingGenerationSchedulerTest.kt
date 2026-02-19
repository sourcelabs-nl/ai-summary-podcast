package com.aisummarypodcast.scheduler

import com.aisummarypodcast.llm.LlmPipeline
import com.aisummarypodcast.llm.PipelineResult
import com.aisummarypodcast.podcast.EpisodeService
import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PodcastRepository
import io.mockk.*
import org.junit.jupiter.api.Test
import java.time.Instant

class BriefingGenerationSchedulerTest {

    private val podcastRepository = mockk<PodcastRepository>()
    private val llmPipeline = mockk<LlmPipeline>()
    private val episodeRepository = mockk<EpisodeRepository>()
    private val episodeService = mockk<EpisodeService>()

    private val scheduler = BriefingGenerationScheduler(
        podcastRepository, llmPipeline, episodeRepository, episodeService
    )

    private fun duePodcast(requireReview: Boolean = false) = Podcast(
        id = "p1", userId = "u1", name = "Test", topic = "tech",
        cron = "0 0 0 * * *",
        requireReview = requireReview,
        lastGeneratedAt = Instant.now().minusSeconds(86400).toString()
    )

    @Test
    fun `skips generation when pending review episode exists`() {
        val podcast = duePodcast(requireReview = true)
        every { podcastRepository.findAll() } returns listOf(podcast)
        every { podcastRepository.findById("p1") } returns java.util.Optional.of(podcast)
        every { episodeRepository.findByPodcastIdAndStatusIn("p1", listOf("PENDING_REVIEW", "APPROVED")) } returns listOf(
            Episode(id = 1, podcastId = "p1", generatedAt = Instant.now().toString(), scriptText = "script", status = "PENDING_REVIEW")
        )

        scheduler.checkAndGenerate()

        verify(exactly = 0) { llmPipeline.run(any()) }
    }

    @Test
    fun `skips generation when approved episode exists`() {
        val podcast = duePodcast(requireReview = true)
        every { podcastRepository.findAll() } returns listOf(podcast)
        every { podcastRepository.findById("p1") } returns java.util.Optional.of(podcast)
        every { episodeRepository.findByPodcastIdAndStatusIn("p1", listOf("PENDING_REVIEW", "APPROVED")) } returns listOf(
            Episode(id = 1, podcastId = "p1", generatedAt = Instant.now().toString(), scriptText = "script", status = "APPROVED")
        )

        scheduler.checkAndGenerate()

        verify(exactly = 0) { llmPipeline.run(any()) }
    }

    @Test
    fun `delegates to EpisodeService when pipeline returns result`() {
        val podcast = duePodcast(requireReview = false)
        val pipelineResult = PipelineResult(
            script = "Generated script", filterModel = "anthropic/claude-haiku-4.5",
            composeModel = "anthropic/claude-sonnet-4"
        )
        val episode = Episode(id = 10, podcastId = "p1", generatedAt = Instant.now().toString(), scriptText = "Generated script")

        every { podcastRepository.findAll() } returns listOf(podcast)
        every { podcastRepository.findById("p1") } returns java.util.Optional.of(podcast)
        every { llmPipeline.run(podcast) } returns pipelineResult
        every { episodeService.createEpisodeFromPipelineResult(podcast, pipelineResult) } returns episode

        scheduler.checkAndGenerate()

        verify { episodeService.createEpisodeFromPipelineResult(podcast, pipelineResult) }
    }

    @Test
    fun `delegates to EpisodeService for review episode`() {
        val podcast = duePodcast(requireReview = true)
        val pipelineResult = PipelineResult(
            script = "Script", filterModel = "filter", composeModel = "compose",
            processedArticleIds = listOf(10L, 20L)
        )
        val episode = Episode(id = 7, podcastId = "p1", generatedAt = Instant.now().toString(), scriptText = "Script", status = "PENDING_REVIEW")

        every { podcastRepository.findAll() } returns listOf(podcast)
        every { podcastRepository.findById("p1") } returns java.util.Optional.of(podcast)
        every { episodeRepository.findByPodcastIdAndStatusIn("p1", listOf("PENDING_REVIEW", "APPROVED")) } returns emptyList()
        every { llmPipeline.run(podcast) } returns pipelineResult
        every { episodeService.createEpisodeFromPipelineResult(podcast, pipelineResult) } returns episode

        scheduler.checkAndGenerate()

        verify { episodeService.createEpisodeFromPipelineResult(podcast, pipelineResult) }
    }

    @Test
    fun `updates lastGeneratedAt when pipeline returns null`() {
        val podcast = duePodcast(requireReview = false)
        every { podcastRepository.findAll() } returns listOf(podcast)
        every { podcastRepository.findById("p1") } returns java.util.Optional.of(podcast)
        every { llmPipeline.run(podcast) } returns null
        every { podcastRepository.save(any()) } answers { firstArg() }

        scheduler.checkAndGenerate()

        verify { podcastRepository.save(match { it.lastGeneratedAt != null }) }
        verify(exactly = 0) { episodeService.createEpisodeFromPipelineResult(any(), any()) }
    }
}

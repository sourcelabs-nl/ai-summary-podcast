package com.aisummarypodcast.scheduler

import com.aisummarypodcast.llm.LlmPipeline
import com.aisummarypodcast.llm.PipelineResult
import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.EpisodeArticle
import com.aisummarypodcast.store.EpisodeArticleRepository
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PodcastRepository
import com.aisummarypodcast.tts.TtsPipeline
import io.mockk.*
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class BriefingGenerationSchedulerTest {

    private val podcastRepository = mockk<PodcastRepository>()
    private val llmPipeline = mockk<LlmPipeline>()
    private val ttsPipeline = mockk<TtsPipeline>()
    private val episodeRepository = mockk<EpisodeRepository>()
    private val episodeArticleRepository = mockk<EpisodeArticleRepository> {
        every { save(any()) } answers { firstArg() }
    }

    private val scheduler = BriefingGenerationScheduler(
        podcastRepository, llmPipeline, ttsPipeline, episodeRepository, episodeArticleRepository
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
    fun `generates pending episode when review required and no pending exists`() {
        val podcast = duePodcast(requireReview = true)
        val pipelineResult = PipelineResult(script = "Generated script", filterModel = "anthropic/claude-haiku-4.5", composeModel = "anthropic/claude-sonnet-4")
        every { podcastRepository.findAll() } returns listOf(podcast)
        every { podcastRepository.findById("p1") } returns java.util.Optional.of(podcast)
        every { episodeRepository.findByPodcastIdAndStatusIn("p1", listOf("PENDING_REVIEW", "APPROVED")) } returns emptyList()
        every { llmPipeline.run(podcast) } returns pipelineResult
        every { episodeRepository.save(any()) } answers { firstArg<Episode>().copy(id = 10) }
        every { podcastRepository.save(any()) } answers { firstArg() }

        scheduler.checkAndGenerate()

        verify { episodeRepository.save(match { it.status == "PENDING_REVIEW" && it.scriptText == "Generated script" && it.filterModel == "anthropic/claude-haiku-4.5" && it.composeModel == "anthropic/claude-sonnet-4" }) }
        verify(exactly = 0) { ttsPipeline.generate(any(), any()) }
    }

    @Test
    fun `generates audio immediately when review not required`() {
        val podcast = duePodcast(requireReview = false)
        val pipelineResult = PipelineResult(script = "Generated script", filterModel = "anthropic/claude-haiku-4.5", composeModel = "anthropic/claude-sonnet-4")
        val episode = Episode(id = 1, podcastId = "p1", generatedAt = Instant.now().toString(), scriptText = "script", audioFilePath = "/audio.mp3", durationSeconds = 60)
        every { podcastRepository.findAll() } returns listOf(podcast)
        every { podcastRepository.findById("p1") } returns java.util.Optional.of(podcast)
        every { llmPipeline.run(podcast) } returns pipelineResult
        every { ttsPipeline.generate("Generated script", podcast) } returns episode
        every { episodeRepository.save(any()) } answers { firstArg<Episode>().copy(id = firstArg<Episode>().id ?: 1) }
        every { podcastRepository.save(any()) } answers { firstArg() }

        scheduler.checkAndGenerate()

        verify { ttsPipeline.generate("Generated script", podcast) }
    }

    @Test
    fun `saves episode-article links after episode creation`() {
        val podcast = duePodcast(requireReview = false)
        val pipelineResult = PipelineResult(
            script = "Script", filterModel = "filter", composeModel = "compose",
            processedArticleIds = listOf(10L, 20L, 30L)
        )
        val episode = Episode(id = 5, podcastId = "p1", generatedAt = Instant.now().toString(), scriptText = "Script", audioFilePath = "/audio.mp3", durationSeconds = 60)
        every { podcastRepository.findAll() } returns listOf(podcast)
        every { podcastRepository.findById("p1") } returns java.util.Optional.of(podcast)
        every { llmPipeline.run(podcast) } returns pipelineResult
        every { ttsPipeline.generate("Script", podcast) } returns episode
        every { episodeRepository.save(any()) } answers { firstArg<Episode>().copy(id = firstArg<Episode>().id ?: 5) }
        every { podcastRepository.save(any()) } answers { firstArg() }

        scheduler.checkAndGenerate()

        verify(exactly = 3) { episodeArticleRepository.save(any()) }
        verify { episodeArticleRepository.save(match { it.episodeId == 5L && it.articleId == 10L }) }
        verify { episodeArticleRepository.save(match { it.episodeId == 5L && it.articleId == 20L }) }
        verify { episodeArticleRepository.save(match { it.episodeId == 5L && it.articleId == 30L }) }
    }

    @Test
    fun `saves episode-article links for review episode`() {
        val podcast = duePodcast(requireReview = true)
        val pipelineResult = PipelineResult(
            script = "Script", filterModel = "filter", composeModel = "compose",
            processedArticleIds = listOf(10L, 20L)
        )
        every { podcastRepository.findAll() } returns listOf(podcast)
        every { podcastRepository.findById("p1") } returns java.util.Optional.of(podcast)
        every { episodeRepository.findByPodcastIdAndStatusIn("p1", listOf("PENDING_REVIEW", "APPROVED")) } returns emptyList()
        every { llmPipeline.run(podcast) } returns pipelineResult
        every { episodeRepository.save(any()) } answers { firstArg<Episode>().copy(id = 7) }
        every { podcastRepository.save(any()) } answers { firstArg() }

        scheduler.checkAndGenerate()

        verify(exactly = 2) { episodeArticleRepository.save(any()) }
        verify { episodeArticleRepository.save(match { it.episodeId == 7L && it.articleId == 10L }) }
        verify { episodeArticleRepository.save(match { it.episodeId == 7L && it.articleId == 20L }) }
    }

    @Test
    fun `no episode-article links saved when processedArticleIds is empty`() {
        val podcast = duePodcast(requireReview = false)
        val pipelineResult = PipelineResult(
            script = "Script", filterModel = "filter", composeModel = "compose",
            processedArticleIds = emptyList()
        )
        val episode = Episode(id = 5, podcastId = "p1", generatedAt = Instant.now().toString(), scriptText = "Script", audioFilePath = "/audio.mp3", durationSeconds = 60)
        every { podcastRepository.findAll() } returns listOf(podcast)
        every { podcastRepository.findById("p1") } returns java.util.Optional.of(podcast)
        every { llmPipeline.run(podcast) } returns pipelineResult
        every { ttsPipeline.generate("Script", podcast) } returns episode
        every { episodeRepository.save(any()) } answers { firstArg<Episode>().copy(id = firstArg<Episode>().id ?: 5) }
        every { podcastRepository.save(any()) } answers { firstArg() }

        scheduler.checkAndGenerate()

        verify(exactly = 0) { episodeArticleRepository.save(any()) }
    }
}

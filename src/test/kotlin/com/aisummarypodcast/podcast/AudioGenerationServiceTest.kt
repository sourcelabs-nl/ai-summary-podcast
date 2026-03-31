package com.aisummarypodcast.podcast

import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.store.EpisodeStatus
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PodcastRepository
import com.aisummarypodcast.tts.TtsPipeline
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.context.ApplicationEventPublisher
import org.junit.jupiter.api.Test
import java.util.*

class AudioGenerationServiceTest {

    private val episodeRepository = mockk<EpisodeRepository>()
    private val podcastRepository = mockk<PodcastRepository>()
    private val ttsPipeline = mockk<TtsPipeline>()
    private val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)

    private val service = AudioGenerationService(
        episodeRepository, podcastRepository, ttsPipeline, eventPublisher
    )

    private val podcast = Podcast(id = "p1", userId = "u1", name = "Test", topic = "tech")

    private val approvedEpisode = Episode(
        id = 1L, podcastId = "p1", generatedAt = "2025-01-01T00:00:00Z",
        scriptText = "Test script", status = EpisodeStatus.APPROVED
    )

    @Test
    fun `sets GENERATING_AUDIO and generates audio on success`() {
        val generatedEpisode = approvedEpisode.copy(status = EpisodeStatus.GENERATED, audioFilePath = "/audio.mp3", durationSeconds = 120)
        every { episodeRepository.findById(1L) } returns Optional.of(approvedEpisode)
        every { podcastRepository.findById("p1") } returns Optional.of(podcast)
        every { episodeRepository.save(any()) } answers { firstArg() }
        every { ttsPipeline.generateForExistingEpisode(approvedEpisode, podcast) } returns generatedEpisode

        service.generateAudioAsync(1L, "p1")

        verify { episodeRepository.save(match { it.status == EpisodeStatus.GENERATING_AUDIO }) }
        verify { ttsPipeline.generateForExistingEpisode(approvedEpisode, podcast) }
    }

    @Test
    fun `updates status to FAILED on TTS error`() {
        every { episodeRepository.findById(1L) } returns Optional.of(approvedEpisode)
        every { podcastRepository.findById("p1") } returns Optional.of(podcast)
        every { ttsPipeline.generateForExistingEpisode(approvedEpisode, podcast) } throws RuntimeException("TTS failure")
        every { episodeRepository.save(any()) } answers { firstArg() }

        service.generateAudioAsync(1L, "p1")

        verify { episodeRepository.save(match { it.status == EpisodeStatus.FAILED }) }
    }

    @Test
    fun `updates status to FAILED when podcast not found`() {
        every { episodeRepository.findById(1L) } returns Optional.of(approvedEpisode)
        every { podcastRepository.findById("p1") } returns Optional.empty()
        every { episodeRepository.save(any()) } answers { firstArg() }

        service.generateAudioAsync(1L, "p1")

        verify { episodeRepository.save(match { it.status == EpisodeStatus.FAILED }) }
    }
}

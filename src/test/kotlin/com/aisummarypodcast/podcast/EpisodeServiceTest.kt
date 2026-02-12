package com.aisummarypodcast.podcast

import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PodcastRepository
import com.aisummarypodcast.tts.TtsPipeline
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.util.*

class EpisodeServiceTest {

    private val episodeRepository = mockk<EpisodeRepository>()
    private val podcastRepository = mockk<PodcastRepository>()
    private val ttsPipeline = mockk<TtsPipeline>()

    private val episodeService = EpisodeService(episodeRepository, podcastRepository, ttsPipeline)

    private val podcast = Podcast(id = "p1", userId = "u1", name = "Test", topic = "tech")
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

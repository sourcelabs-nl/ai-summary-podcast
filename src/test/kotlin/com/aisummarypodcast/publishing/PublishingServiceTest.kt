package com.aisummarypodcast.publishing

import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.EpisodePublication
import com.aisummarypodcast.store.EpisodePublicationRepository
import com.aisummarypodcast.store.EpisodeStatus
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PublicationStatus
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PublishingServiceTest {

    private val publisher = mockk<EpisodePublisher> {
        every { targetName() } returns "soundcloud"
    }
    private val registry = PublisherRegistry(listOf(publisher))
    private val publicationRepository = mockk<EpisodePublicationRepository>(relaxed = true)
    private val service = PublishingService(registry, publicationRepository)

    private val podcast = Podcast(id = "pod1", userId = "user1", name = "Test Pod", topic = "tech")
    private val episode = Episode(
        id = 1L,
        podcastId = "pod1",
        generatedAt = "2026-02-13T10:00:00Z",
        scriptText = "Test script",
        status = EpisodeStatus.GENERATED,
        audioFilePath = "/tmp/test.mp3"
    )

    @Test
    fun `publish succeeds for valid episode`() {
        every { publicationRepository.findByEpisodeIdAndTarget(1L, "soundcloud") } returns null
        every { publicationRepository.save(any()) } answers { firstArg<EpisodePublication>().copy(id = 10L) }
        every { publisher.publish(episode, podcast, "user1") } returns PublishResult("sc-123", "https://soundcloud.com/track/123")

        val result = service.publish(episode, podcast, "user1", "soundcloud")

        assertEquals(PublicationStatus.PUBLISHED, result.status)
        assertEquals("sc-123", result.externalId)
    }

    @Test
    fun `publish throws for unknown target`() {
        assertThrows<IllegalArgumentException> {
            service.publish(episode, podcast, "user1", "youtube")
        }
    }

    @Test
    fun `publish throws when episode not generated`() {
        val pendingEpisode = episode.copy(status = EpisodeStatus.PENDING_REVIEW)

        assertThrows<IllegalStateException> {
            service.publish(pendingEpisode, podcast, "user1", "soundcloud")
        }
    }

    @Test
    fun `publish throws when episode has no audio`() {
        val noAudioEpisode = episode.copy(audioFilePath = null)

        assertThrows<IllegalStateException> {
            service.publish(noAudioEpisode, podcast, "user1", "soundcloud")
        }
    }

    @Test
    fun `publish throws when already published`() {
        val existing = EpisodePublication(
            id = 5L,
            episodeId = 1L,
            target = "soundcloud",
            status = PublicationStatus.PUBLISHED,
            createdAt = "2026-02-13T10:00:00Z"
        )
        every { publicationRepository.findByEpisodeIdAndTarget(1L, "soundcloud") } returns existing

        assertThrows<IllegalStateException> {
            service.publish(episode, podcast, "user1", "soundcloud")
        }
    }

    @Test
    fun `publish records failure when publisher throws`() {
        every { publicationRepository.findByEpisodeIdAndTarget(1L, "soundcloud") } returns null
        every { publicationRepository.save(any()) } answers { firstArg<EpisodePublication>().copy(id = 10L) }
        every { publisher.publish(episode, podcast, "user1") } throws RuntimeException("API error")

        assertThrows<RuntimeException> {
            service.publish(episode, podcast, "user1", "soundcloud")
        }
    }
}

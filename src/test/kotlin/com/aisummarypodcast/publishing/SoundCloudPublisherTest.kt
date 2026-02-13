package com.aisummarypodcast.publishing

import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.Podcast
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SoundCloudPublisherTest {

    private val soundCloudClient = mockk<SoundCloudClient>()
    private val tokenManager = mockk<SoundCloudTokenManager>()
    private val publisher = SoundCloudPublisher(soundCloudClient, tokenManager)

    private val podcast = Podcast(id = "pod1", userId = "user1", name = "Tech News", topic = "AI, machine learning")
    private val episode = Episode(
        id = 1L,
        podcastId = "pod1",
        generatedAt = "2026-02-13T10:00:00Z",
        scriptText = "This is the episode script about AI and tech news.",
        status = "GENERATED",
        audioFilePath = "/tmp/test.mp3"
    )

    @Test
    fun `targetName returns soundcloud`() {
        assertEquals("soundcloud", publisher.targetName())
    }

    @Test
    fun `publish uploads track with correct metadata`() {
        every { tokenManager.getValidAccessToken("user1") } returns "access-token"

        val requestSlot = slot<TrackUploadRequest>()
        every { soundCloudClient.uploadTrack("access-token", capture(requestSlot)) } returns
            SoundCloudTrackResponse(id = 456, permalinkUrl = "https://soundcloud.com/user/tech-news-2026-02-13")

        val result = publisher.publish(episode, podcast, "user1")

        assertEquals("456", result.externalId)
        assertEquals("https://soundcloud.com/user/tech-news-2026-02-13", result.externalUrl)

        val uploadRequest = requestSlot.captured
        assertEquals("Tech News - 2026-02-13", uploadRequest.title)
        assertEquals("This is the episode script about AI and tech news.", uploadRequest.description)
        assertEquals("AI \"machine learning\"", uploadRequest.tagList)
    }

    @Test
    fun `publish throws when no OAuth connection`() {
        every { tokenManager.getValidAccessToken("user1") } throws
            IllegalStateException("No SoundCloud connection found")

        assertThrows<IllegalStateException> {
            publisher.publish(episode, podcast, "user1")
        }
    }

    @Test
    fun `publish truncates description to 500 chars`() {
        val longScript = "A".repeat(600)
        val longEpisode = episode.copy(scriptText = longScript)

        every { tokenManager.getValidAccessToken("user1") } returns "access-token"
        val requestSlot = slot<TrackUploadRequest>()
        every { soundCloudClient.uploadTrack("access-token", capture(requestSlot)) } returns
            SoundCloudTrackResponse(id = 1, permalinkUrl = "https://soundcloud.com/track")

        publisher.publish(longEpisode, podcast, "user1")

        assertEquals(500, requestSlot.captured.description.length)
    }

    @Test
    fun `buildTagList handles multi-word tags`() {
        val podcastWithTags = podcast.copy(topic = "AI, machine learning, Kotlin")
        every { tokenManager.getValidAccessToken("user1") } returns "access-token"

        val requestSlot = slot<TrackUploadRequest>()
        every { soundCloudClient.uploadTrack("access-token", capture(requestSlot)) } returns
            SoundCloudTrackResponse(id = 1, permalinkUrl = "https://soundcloud.com/track")

        publisher.publish(episode, podcastWithTags, "user1")

        assertEquals("AI \"machine learning\" Kotlin", requestSlot.captured.tagList)
    }
}

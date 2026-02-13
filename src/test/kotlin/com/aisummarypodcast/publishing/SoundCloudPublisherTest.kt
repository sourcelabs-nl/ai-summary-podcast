package com.aisummarypodcast.publishing

import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PodcastRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.client.HttpClientErrorException
import org.springframework.http.HttpStatusCode

class SoundCloudPublisherTest {

    private val soundCloudClient = mockk<SoundCloudClient>()
    private val tokenManager = mockk<SoundCloudTokenManager>()
    private val podcastRepository = mockk<PodcastRepository> {
        every { save(any()) } answers { firstArg() }
    }
    private val publisher = SoundCloudPublisher(soundCloudClient, tokenManager, podcastRepository)

    private val podcast = Podcast(id = "pod1", userId = "user1", name = "Tech News", topic = "AI, machine learning")
    private val episode = Episode(
        id = 1L,
        podcastId = "pod1",
        generatedAt = "2026-02-13T10:00:00Z",
        scriptText = "This is the episode script about AI and tech news.",
        status = "GENERATED",
        audioFilePath = "/tmp/test.mp3"
    )

    private val trackResponse = SoundCloudTrackResponse(id = 456, permalinkUrl = "https://soundcloud.com/user/tech-news")
    private val playlistResponse = SoundCloudPlaylistResponse(id = 789)

    @Test
    fun `targetName returns soundcloud`() {
        assertEquals("soundcloud", publisher.targetName())
    }

    @Test
    fun `publish uploads track with correct metadata`() {
        every { tokenManager.getValidAccessToken("user1") } returns "access-token"
        val requestSlot = slot<TrackUploadRequest>()
        every { soundCloudClient.uploadTrack("access-token", capture(requestSlot)) } returns trackResponse
        every { soundCloudClient.createPlaylist("access-token", "Tech News", listOf(456)) } returns playlistResponse

        val result = publisher.publish(episode, podcast, "user1")

        assertEquals("456", result.externalId)
        assertEquals("https://soundcloud.com/user/tech-news", result.externalUrl)

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
        every { soundCloudClient.uploadTrack("access-token", capture(requestSlot)) } returns trackResponse
        every { soundCloudClient.createPlaylist("access-token", "Tech News", listOf(456)) } returns playlistResponse

        publisher.publish(longEpisode, podcast, "user1")

        assertEquals(500, requestSlot.captured.description.length)
    }

    @Test
    fun `buildTagList handles multi-word tags`() {
        val podcastWithTags = podcast.copy(topic = "AI, machine learning, Kotlin")
        every { tokenManager.getValidAccessToken("user1") } returns "access-token"
        val requestSlot = slot<TrackUploadRequest>()
        every { soundCloudClient.uploadTrack("access-token", capture(requestSlot)) } returns trackResponse
        every { soundCloudClient.createPlaylist("access-token", "Tech News", listOf(456)) } returns playlistResponse

        publisher.publish(episode, podcastWithTags, "user1")

        assertEquals("AI \"machine learning\" Kotlin", requestSlot.captured.tagList)
    }

    @Test
    fun `first publish creates playlist and stores ID`() {
        every { tokenManager.getValidAccessToken("user1") } returns "access-token"
        every { soundCloudClient.uploadTrack("access-token", any()) } returns trackResponse
        every { soundCloudClient.createPlaylist("access-token", "Tech News", listOf(456)) } returns playlistResponse

        publisher.publish(episode, podcast, "user1")

        val savedPodcast = slot<Podcast>()
        verify { podcastRepository.save(capture(savedPodcast)) }
        assertEquals("789", savedPodcast.captured.soundcloudPlaylistId)
    }

    @Test
    fun `subsequent publish adds track to existing playlist`() {
        val podcastWithPlaylist = podcast.copy(soundcloudPlaylistId = "123")
        every { tokenManager.getValidAccessToken("user1") } returns "access-token"
        every { soundCloudClient.uploadTrack("access-token", any()) } returns trackResponse
        every { soundCloudClient.addTrackToPlaylist("access-token", 123, listOf(456)) } returns playlistResponse

        publisher.publish(episode, podcastWithPlaylist, "user1")

        verify { soundCloudClient.addTrackToPlaylist("access-token", 123, listOf(456)) }
        verify(exactly = 0) { soundCloudClient.createPlaylist(any(), any(), any()) }
    }

    @Test
    fun `stale playlist triggers recreation`() {
        val podcastWithStalePlaylist = podcast.copy(soundcloudPlaylistId = "999")
        every { tokenManager.getValidAccessToken("user1") } returns "access-token"
        every { soundCloudClient.uploadTrack("access-token", any()) } returns trackResponse
        every { soundCloudClient.addTrackToPlaylist("access-token", 999, listOf(456)) } throws
            HttpClientErrorException.create(HttpStatusCode.valueOf(404), "Not Found", org.springframework.http.HttpHeaders(), ByteArray(0), null)
        every { soundCloudClient.createPlaylist("access-token", "Tech News", listOf(456)) } returns playlistResponse

        publisher.publish(episode, podcastWithStalePlaylist, "user1")

        val savedPodcast = slot<Podcast>()
        verify { podcastRepository.save(capture(savedPodcast)) }
        assertEquals("789", savedPodcast.captured.soundcloudPlaylistId)
    }
}

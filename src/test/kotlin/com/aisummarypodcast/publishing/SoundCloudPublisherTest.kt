package com.aisummarypodcast.publishing

import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PodcastRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import com.aisummarypodcast.store.EpisodePublication
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
        assertEquals("tech-news-2026-02-13", uploadRequest.permalink)
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
    fun `subsequent publish fetches existing tracks and appends new track`() {
        val podcastWithPlaylist = podcast.copy(soundcloudPlaylistId = "123")
        val existingPlaylist = SoundCloudPlaylistDetailResponse(
            id = 123,
            tracks = listOf(SoundCloudPlaylistTrack(100), SoundCloudPlaylistTrack(200))
        )
        every { tokenManager.getValidAccessToken("user1") } returns "access-token"
        every { soundCloudClient.uploadTrack("access-token", any()) } returns trackResponse
        every { soundCloudClient.getPlaylist("access-token", 123) } returns existingPlaylist
        every { soundCloudClient.addTrackToPlaylist("access-token", 123, listOf(100, 200, 456)) } returns playlistResponse

        publisher.publish(episode, podcastWithPlaylist, "user1")

        verify { soundCloudClient.getPlaylist("access-token", 123) }
        verify { soundCloudClient.addTrackToPlaylist("access-token", 123, listOf(100, 200, 456)) }
        verify(exactly = 0) { soundCloudClient.createPlaylist(any(), any(), any()) }
    }

    @Test
    fun `rebuildPlaylist sets all track IDs on existing playlist`() {
        val podcastWithPlaylist = podcast.copy(soundcloudPlaylistId = "123")
        every { tokenManager.getValidAccessToken("user1") } returns "access-token"
        every { soundCloudClient.addTrackToPlaylist("access-token", 123, listOf(100, 200, 300)) } returns playlistResponse

        publisher.rebuildPlaylist(podcastWithPlaylist, "user1", listOf(100, 200, 300))

        verify { soundCloudClient.addTrackToPlaylist("access-token", 123, listOf(100, 200, 300)) }
        verify(exactly = 0) { soundCloudClient.createPlaylist(any(), any(), any()) }
    }

    @Test
    fun `rebuildPlaylist creates new playlist when no playlist ID exists`() {
        every { tokenManager.getValidAccessToken("user1") } returns "access-token"
        every { soundCloudClient.createPlaylist("access-token", "Tech News", listOf(100, 200)) } returns playlistResponse

        publisher.rebuildPlaylist(podcast, "user1", listOf(100, 200))

        verify { soundCloudClient.createPlaylist("access-token", "Tech News", listOf(100, 200)) }
        val savedPodcast = slot<Podcast>()
        verify { podcastRepository.save(capture(savedPodcast)) }
        assertEquals("789", savedPodcast.captured.soundcloudPlaylistId)
    }

    @Test
    fun `rebuildPlaylist creates new playlist when existing playlist returns 404`() {
        val podcastWithStalePlaylist = podcast.copy(soundcloudPlaylistId = "999")
        every { tokenManager.getValidAccessToken("user1") } returns "access-token"
        every { soundCloudClient.addTrackToPlaylist("access-token", 999, listOf(100)) } throws
            HttpClientErrorException.create(HttpStatusCode.valueOf(404), "Not Found", org.springframework.http.HttpHeaders(), ByteArray(0), null)
        every { soundCloudClient.createPlaylist("access-token", "Tech News", listOf(100)) } returns playlistResponse

        publisher.rebuildPlaylist(podcastWithStalePlaylist, "user1", listOf(100))

        val savedPodcast = slot<Podcast>()
        verify { podcastRepository.save(capture(savedPodcast)) }
        assertEquals("789", savedPodcast.captured.soundcloudPlaylistId)
    }

    @Test
    fun `updateTrackPermalinks updates each track with correct permalink`() {
        val episode1 = episode.copy(id = 1L, generatedAt = "2026-02-13T10:00:00Z")
        val episode2 = episode.copy(id = 2L, generatedAt = "2026-02-14T10:00:00Z")
        val pub1 = EpisodePublication(id = 10, episodeId = 1L, target = "soundcloud", status = "PUBLISHED", externalId = "100", createdAt = "2026-02-13T10:00:00Z")
        val pub2 = EpisodePublication(id = 11, episodeId = 2L, target = "soundcloud", status = "PUBLISHED", externalId = "200", createdAt = "2026-02-14T10:00:00Z")

        every { tokenManager.getValidAccessToken("user1") } returns "access-token"
        every { soundCloudClient.updateTrack("access-token", 100, "tech-news-2026-02-13") } returns trackResponse
        every { soundCloudClient.updateTrack("access-token", 200, "tech-news-2026-02-14") } returns trackResponse

        publisher.updateTrackPermalinks(podcast, "user1", listOf(episode1, episode2), listOf(pub1, pub2))

        verify { soundCloudClient.updateTrack("access-token", 100, "tech-news-2026-02-13") }
        verify { soundCloudClient.updateTrack("access-token", 200, "tech-news-2026-02-14") }
    }

    @Test
    fun `stale playlist triggers recreation when getPlaylist returns 404`() {
        val podcastWithStalePlaylist = podcast.copy(soundcloudPlaylistId = "999")
        every { tokenManager.getValidAccessToken("user1") } returns "access-token"
        every { soundCloudClient.uploadTrack("access-token", any()) } returns trackResponse
        every { soundCloudClient.getPlaylist("access-token", 999) } throws
            HttpClientErrorException.create(HttpStatusCode.valueOf(404), "Not Found", org.springframework.http.HttpHeaders(), ByteArray(0), null)
        every { soundCloudClient.createPlaylist("access-token", "Tech News", listOf(456)) } returns playlistResponse

        publisher.publish(episode, podcastWithStalePlaylist, "user1")

        val savedPodcast = slot<Podcast>()
        verify { podcastRepository.save(capture(savedPodcast)) }
        assertEquals("789", savedPodcast.captured.soundcloudPlaylistId)
    }
}

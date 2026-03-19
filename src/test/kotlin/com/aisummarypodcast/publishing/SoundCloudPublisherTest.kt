package com.aisummarypodcast.publishing

import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.EpisodePublication
import com.aisummarypodcast.store.EpisodeStatus
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PodcastPublicationTarget
import com.aisummarypodcast.store.PublicationStatus
import tools.jackson.databind.json.JsonMapper
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

    private val soundCloudClient = mockk<SoundCloudClient> {
        every { getMe(any()) } returns SoundCloudMeResponse(
            id = 1L,
            username = "testuser",
            plan = "Free",
            quota = SoundCloudQuota(unlimitedUploadQuota = false, uploadSecondsUsed = 1000, uploadSecondsLeft = 5000)
        )
    }
    private val tokenManager = mockk<SoundCloudTokenManager>()
    private val targetService = mockk<PodcastPublicationTargetService>(relaxed = true) {
        every { get("pod1", "soundcloud") } returns null
        every { upsert(any(), any(), any(), any()) } answers {
            PodcastPublicationTarget(id = 1, podcastId = arg(0), target = arg(1), config = arg(2), enabled = arg(3))
        }
    }
    private val objectMapper = JsonMapper()
    private val publisher = SoundCloudPublisher(soundCloudClient, tokenManager, targetService, objectMapper)

    private val podcast = Podcast(id = "pod1", userId = "user1", name = "Tech News", topic = "AI, machine learning")
    private val episode = Episode(
        id = 1L,
        podcastId = "pod1",
        generatedAt = "2026-02-13T10:00:00Z",
        scriptText = "This is the episode script about AI and tech news.",
        status = EpisodeStatus.GENERATED,
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

        publisher.publish(longEpisode, podcast, "user1")

        assertEquals(500, requestSlot.captured.description.length)
    }

    @Test
    fun `publish uses show notes for description when available`() {
        val episodeWithNotes = episode.copy(
            showNotes = "Recap.\n\nSources:\n- Article\n  https://example.com/1"
        )

        every { tokenManager.getValidAccessToken("user1") } returns "access-token"
        val requestSlot = slot<TrackUploadRequest>()
        every { soundCloudClient.uploadTrack("access-token", capture(requestSlot)) } returns trackResponse

        publisher.publish(episodeWithNotes, podcast, "user1")

        assertEquals("Recap.\n\nSources:\n- Article\n  https://example.com/1", requestSlot.captured.description)
    }

    @Test
    fun `publish uses recap when no show notes`() {
        val episodeWithRecap = episode.copy(recap = "A short recap")

        every { tokenManager.getValidAccessToken("user1") } returns "access-token"
        val requestSlot = slot<TrackUploadRequest>()
        every { soundCloudClient.uploadTrack("access-token", capture(requestSlot)) } returns trackResponse

        publisher.publish(episodeWithRecap, podcast, "user1")

        assertEquals("A short recap", requestSlot.captured.description)
    }

    @Test
    fun `buildTagList handles multi-word tags`() {
        val podcastWithTags = podcast.copy(topic = "AI, machine learning, Kotlin")
        every { tokenManager.getValidAccessToken("user1") } returns "access-token"
        val requestSlot = slot<TrackUploadRequest>()
        every { soundCloudClient.uploadTrack("access-token", capture(requestSlot)) } returns trackResponse

        publisher.publish(episode, podcastWithTags, "user1")

        assertEquals("AI \"machine learning\" Kotlin", requestSlot.captured.tagList)
    }

    @Test
    fun `rebuildPlaylist sets all track IDs on existing playlist`() {
        every { targetService.get("pod1", "soundcloud") } returns
            PodcastPublicationTarget(id = 1, podcastId = "pod1", target = "soundcloud", config = """{"playlistId":"123"}""", enabled = true)
        every { tokenManager.getValidAccessToken("user1") } returns "access-token"
        every { soundCloudClient.addTrackToPlaylist("access-token", 123, listOf(100, 200, 300)) } returns playlistResponse

        publisher.rebuildPlaylist(podcast, "user1", listOf(100, 200, 300))

        verify { soundCloudClient.addTrackToPlaylist("access-token", 123, listOf(100, 200, 300)) }
        verify(exactly = 0) { soundCloudClient.createPlaylist(any(), any(), any()) }
    }

    @Test
    fun `rebuildPlaylist creates new playlist when no playlist ID exists`() {
        every { tokenManager.getValidAccessToken("user1") } returns "access-token"
        every { soundCloudClient.createPlaylist("access-token", "Tech News", listOf(100, 200)) } returns playlistResponse

        publisher.rebuildPlaylist(podcast, "user1", listOf(100, 200))

        verify { soundCloudClient.createPlaylist("access-token", "Tech News", listOf(100, 200)) }
        verify { targetService.upsert("pod1", "soundcloud", match { it.contains("789") }, any()) }
    }

    @Test
    fun `rebuildPlaylist creates new playlist when existing playlist returns 404`() {
        every { targetService.get("pod1", "soundcloud") } returns
            PodcastPublicationTarget(id = 1, podcastId = "pod1", target = "soundcloud", config = """{"playlistId":"999"}""", enabled = true)
        every { tokenManager.getValidAccessToken("user1") } returns "access-token"
        every { soundCloudClient.addTrackToPlaylist("access-token", 999, listOf(100)) } throws
            HttpClientErrorException.create(HttpStatusCode.valueOf(404), "Not Found", org.springframework.http.HttpHeaders(), ByteArray(0), null)
        every { soundCloudClient.createPlaylist("access-token", "Tech News", listOf(100)) } returns playlistResponse

        publisher.rebuildPlaylist(podcast, "user1", listOf(100))

        verify { targetService.upsert("pod1", "soundcloud", match { it.contains("789") }, true) }
    }

    @Test
    fun `updateTrackPermalinks updates each track with correct permalink`() {
        val episode1 = episode.copy(id = 1L, generatedAt = "2026-02-13T10:00:00Z")
        val episode2 = episode.copy(id = 2L, generatedAt = "2026-02-14T10:00:00Z")
        val pub1 = EpisodePublication(id = 10, episodeId = 1L, target = "soundcloud", status = PublicationStatus.PUBLISHED, externalId = "100", createdAt = "2026-02-13T10:00:00Z")
        val pub2 = EpisodePublication(id = 11, episodeId = 2L, target = "soundcloud", status = PublicationStatus.PUBLISHED, externalId = "200", createdAt = "2026-02-14T10:00:00Z")

        every { tokenManager.getValidAccessToken("user1") } returns "access-token"
        val expectedDescription = episode.scriptText.take(500)
        every { soundCloudClient.updateTrack("access-token", 100, "tech-news-2026-02-13", expectedDescription) } returns trackResponse
        every { soundCloudClient.updateTrack("access-token", 200, "tech-news-2026-02-14", expectedDescription) } returns trackResponse

        publisher.updateTrackPermalinks(podcast, "user1", listOf(episode1, episode2), listOf(pub1, pub2))

        verify { soundCloudClient.updateTrack("access-token", 100, "tech-news-2026-02-13", expectedDescription) }
        verify { soundCloudClient.updateTrack("access-token", 200, "tech-news-2026-02-14", expectedDescription) }
    }

    @Test
    fun `update calls updateTrack with show notes description`() {
        val episodeWithNotes = episode.copy(
            showNotes = "Recap.\n\nSources:\n- Article\n  https://example.com/1"
        )
        every { tokenManager.getValidAccessToken("user1") } returns "access-token"
        every { soundCloudClient.updateTrack("access-token", 456, description = "Recap.\n\nSources:\n- Article\n  https://example.com/1") } returns trackResponse

        val result = publisher.update(episodeWithNotes, podcast, "user1", "456")

        assertEquals("456", result.externalId)
        assertEquals("https://soundcloud.com/user/tech-news", result.externalUrl)
        verify { soundCloudClient.updateTrack("access-token", 456, description = "Recap.\n\nSources:\n- Article\n  https://example.com/1") }
    }

    @Test
    fun `update falls back to recap when no show notes`() {
        val episodeWithRecap = episode.copy(recap = "Short recap")
        every { tokenManager.getValidAccessToken("user1") } returns "access-token"
        every { soundCloudClient.updateTrack("access-token", 456, description = "Short recap") } returns trackResponse

        val result = publisher.update(episodeWithRecap, podcast, "user1", "456")

        assertEquals("456", result.externalId)
    }

    @Test
    fun `publish throws when quota exceeded`() {
        every { tokenManager.getValidAccessToken("user1") } returns "access-token"
        every { soundCloudClient.getMe("access-token") } returns SoundCloudMeResponse(
            id = 1L, username = "testuser", plan = "Free",
            quota = SoundCloudQuota(unlimitedUploadQuota = false, uploadSecondsUsed = 7000, uploadSecondsLeft = -500)
        )

        val ex = assertThrows<SoundCloudQuotaExceededException> {
            publisher.publish(episode, podcast, "user1")
        }
        assertEquals(true, ex.message?.contains("quota exceeded"))
    }

    @Test
    fun `publish skips quota check when unlimited`() {
        every { tokenManager.getValidAccessToken("user1") } returns "access-token"
        every { soundCloudClient.getMe("access-token") } returns SoundCloudMeResponse(
            id = 1L, username = "testuser", plan = "Pro",
            quota = SoundCloudQuota(unlimitedUploadQuota = true, uploadSecondsUsed = 99999, uploadSecondsLeft = -500)
        )
        every { soundCloudClient.uploadTrack("access-token", any()) } returns trackResponse

        val result = publisher.publish(episode, podcast, "user1")
        assertEquals("456", result.externalId)
    }

}

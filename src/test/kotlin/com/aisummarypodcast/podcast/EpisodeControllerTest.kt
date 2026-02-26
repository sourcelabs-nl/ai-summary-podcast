package com.aisummarypodcast.podcast

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.store.EpisodeStatus
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.User
import com.aisummarypodcast.user.UserService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.*

@WebMvcTest(EpisodeController::class)
class EpisodeControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var episodeRepository: EpisodeRepository

    @MockkBean
    private lateinit var podcastService: PodcastService

    @MockkBean
    private lateinit var userService: UserService

    @MockkBean
    private lateinit var episodeService: EpisodeService

    @MockkBean(relaxed = true)
    private lateinit var appProperties: AppProperties

    private val userId = "user-1"
    private val user = User(id = userId, name = "Test User")
    private val podcastId = "podcast-1"
    private val podcast = Podcast(id = podcastId, userId = userId, name = "Test", topic = "tech")

    private val pendingEpisode = Episode(
        id = 1L, podcastId = podcastId, generatedAt = "2025-01-01T00:00:00Z",
        scriptText = "Test script", status = EpisodeStatus.PENDING_REVIEW
    )

    private val generatedEpisode = Episode(
        id = 2L, podcastId = podcastId, generatedAt = "2025-01-01T00:00:00Z",
        scriptText = "Test script", status = EpisodeStatus.GENERATED,
        audioFilePath = "/audio/test.mp3", durationSeconds = 120
    )

    @Test
    fun `list episodes returns all episodes`() {
        every { userService.findById(userId) } returns user
        every { podcastService.findById(podcastId) } returns podcast
        every { episodeRepository.findByPodcastId(podcastId) } returns listOf(pendingEpisode, generatedEpisode)

        mockMvc.perform(get("/users/$userId/podcasts/$podcastId/episodes"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `list episodes with status filter`() {
        every { userService.findById(userId) } returns user
        every { podcastService.findById(podcastId) } returns podcast
        every { episodeRepository.findByPodcastIdAndStatus(podcastId, "PENDING_REVIEW") } returns listOf(pendingEpisode)

        mockMvc.perform(get("/users/$userId/podcasts/$podcastId/episodes?status=PENDING_REVIEW"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].status").value("PENDING_REVIEW"))
    }

    @Test
    fun `list episodes for non-existing podcast returns 404`() {
        every { userService.findById(userId) } returns user
        every { podcastService.findById(podcastId) } returns null

        mockMvc.perform(get("/users/$userId/podcasts/$podcastId/episodes"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `get single episode`() {
        every { userService.findById(userId) } returns user
        every { podcastService.findById(podcastId) } returns podcast
        every { episodeRepository.findById(1L) } returns Optional.of(pendingEpisode)

        mockMvc.perform(get("/users/$userId/podcasts/$podcastId/episodes/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.scriptText").value("Test script"))
            .andExpect(jsonPath("$.status").value("PENDING_REVIEW"))
    }

    @Test
    fun `get non-existing episode returns 404`() {
        every { userService.findById(userId) } returns user
        every { podcastService.findById(podcastId) } returns podcast
        every { episodeRepository.findById(99L) } returns Optional.empty()

        mockMvc.perform(get("/users/$userId/podcasts/$podcastId/episodes/99"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `edit script of pending episode`() {
        every { userService.findById(userId) } returns user
        every { podcastService.findById(podcastId) } returns podcast
        every { episodeRepository.findById(1L) } returns Optional.of(pendingEpisode)
        every { episodeService.updateScript(pendingEpisode, "Updated script") } returns pendingEpisode.copy(scriptText = "Updated script")

        mockMvc.perform(
            put("/users/$userId/podcasts/$podcastId/episodes/1/script")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"scriptText":"Updated script"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.scriptText").value("Updated script"))
    }

    @Test
    fun `edit script of non-pending episode returns 409`() {
        every { userService.findById(userId) } returns user
        every { podcastService.findById(podcastId) } returns podcast
        every { episodeRepository.findById(2L) } returns Optional.of(generatedEpisode)

        mockMvc.perform(
            put("/users/$userId/podcasts/$podcastId/episodes/2/script")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"scriptText":"Updated script"}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.error").exists())
    }

    @Test
    fun `approve pending episode returns 202`() {
        every { userService.findById(userId) } returns user
        every { podcastService.findById(podcastId) } returns podcast
        every { episodeRepository.findById(1L) } returns Optional.of(pendingEpisode)
        justRun { episodeService.approveAndGenerateAudio(pendingEpisode, podcastId) }

        mockMvc.perform(post("/users/$userId/podcasts/$podcastId/episodes/1/approve"))
            .andExpect(status().isAccepted)

        verify { episodeService.approveAndGenerateAudio(pendingEpisode, podcastId) }
    }

    @Test
    fun `approve failed episode returns 202`() {
        val failedEpisode = pendingEpisode.copy(status = EpisodeStatus.FAILED)
        every { userService.findById(userId) } returns user
        every { podcastService.findById(podcastId) } returns podcast
        every { episodeRepository.findById(1L) } returns Optional.of(failedEpisode)
        justRun { episodeService.approveAndGenerateAudio(failedEpisode, podcastId) }

        mockMvc.perform(post("/users/$userId/podcasts/$podcastId/episodes/1/approve"))
            .andExpect(status().isAccepted)
    }

    @Test
    fun `approve generated episode returns 409`() {
        every { userService.findById(userId) } returns user
        every { podcastService.findById(podcastId) } returns podcast
        every { episodeRepository.findById(2L) } returns Optional.of(generatedEpisode)

        mockMvc.perform(post("/users/$userId/podcasts/$podcastId/episodes/2/approve"))
            .andExpect(status().isConflict)
    }

    @Test
    fun `discard pending episode delegates to service`() {
        every { userService.findById(userId) } returns user
        every { podcastService.findById(podcastId) } returns podcast
        every { episodeRepository.findById(1L) } returns Optional.of(pendingEpisode)
        justRun { episodeService.discardAndResetArticles(pendingEpisode) }

        mockMvc.perform(post("/users/$userId/podcasts/$podcastId/episodes/1/discard"))
            .andExpect(status().isOk)

        verify { episodeService.discardAndResetArticles(pendingEpisode) }
    }

    @Test
    fun `discard non-pending episode returns 409`() {
        every { userService.findById(userId) } returns user
        every { podcastService.findById(podcastId) } returns podcast
        every { episodeRepository.findById(2L) } returns Optional.of(generatedEpisode)

        mockMvc.perform(post("/users/$userId/podcasts/$podcastId/episodes/2/discard"))
            .andExpect(status().isConflict)
    }
}

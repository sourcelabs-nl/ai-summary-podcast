package com.aisummarypodcast.podcast

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.EpisodeArticle
import com.aisummarypodcast.store.EpisodeArticleRepository
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.User
import com.aisummarypodcast.user.UserService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import io.mockk.slot
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

    @MockkBean
    private lateinit var episodeArticleRepository: EpisodeArticleRepository

    @MockkBean
    private lateinit var articleRepository: ArticleRepository

    @MockkBean(relaxed = true)
    private lateinit var appProperties: AppProperties

    private val userId = "user-1"
    private val user = User(id = userId, name = "Test User")
    private val podcastId = "podcast-1"
    private val podcast = Podcast(id = podcastId, userId = userId, name = "Test", topic = "tech")

    private val pendingEpisode = Episode(
        id = 1L, podcastId = podcastId, generatedAt = "2025-01-01T00:00:00Z",
        scriptText = "Test script", status = "PENDING_REVIEW"
    )

    private val generatedEpisode = Episode(
        id = 2L, podcastId = podcastId, generatedAt = "2025-01-01T00:00:00Z",
        scriptText = "Test script", status = "GENERATED",
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
        every { episodeRepository.save(any()) } answers { firstArg() }

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
        every { episodeRepository.save(any()) } answers { firstArg() }
        justRun { episodeService.generateAudioAsync(1L, podcastId) }

        mockMvc.perform(post("/users/$userId/podcasts/$podcastId/episodes/1/approve"))
            .andExpect(status().isAccepted)

        verify { episodeService.generateAudioAsync(1L, podcastId) }
    }

    @Test
    fun `approve failed episode returns 202`() {
        val failedEpisode = pendingEpisode.copy(status = "FAILED")
        every { userService.findById(userId) } returns user
        every { podcastService.findById(podcastId) } returns podcast
        every { episodeRepository.findById(1L) } returns Optional.of(failedEpisode)
        every { episodeRepository.save(any()) } answers { firstArg() }
        justRun { episodeService.generateAudioAsync(1L, podcastId) }

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
    fun `discard pending episode`() {
        every { userService.findById(userId) } returns user
        every { podcastService.findById(podcastId) } returns podcast
        every { episodeRepository.findById(1L) } returns Optional.of(pendingEpisode)
        every { episodeRepository.save(any()) } answers { firstArg() }
        every { episodeArticleRepository.findByEpisodeId(1L) } returns emptyList()

        mockMvc.perform(post("/users/$userId/podcasts/$podcastId/episodes/1/discard"))
            .andExpect(status().isOk)
    }

    @Test
    fun `discard pending episode resets linked articles for reprocessing`() {
        val article1 = Article(
            id = 10L, sourceId = "src-1", title = "Article 1", body = "body",
            url = "https://example.com/1", contentHash = "hash1", isProcessed = true
        )
        val article2 = Article(
            id = 20L, sourceId = "src-1", title = "Article 2", body = "body",
            url = "https://example.com/2", contentHash = "hash2", isProcessed = true
        )
        val links = listOf(
            EpisodeArticle(id = 1L, episodeId = 1L, articleId = 10L),
            EpisodeArticle(id = 2L, episodeId = 1L, articleId = 20L)
        )

        every { userService.findById(userId) } returns user
        every { podcastService.findById(podcastId) } returns podcast
        every { episodeRepository.findById(1L) } returns Optional.of(pendingEpisode)
        every { episodeRepository.save(any()) } answers { firstArg() }
        every { episodeArticleRepository.findByEpisodeId(1L) } returns links
        every { articleRepository.findById(10L) } returns Optional.of(article1)
        every { articleRepository.findById(20L) } returns Optional.of(article2)
        val savedArticles = mutableListOf<Article>()
        every { articleRepository.save(any()) } answers {
            val article = firstArg<Article>()
            savedArticles.add(article)
            article
        }

        mockMvc.perform(post("/users/$userId/podcasts/$podcastId/episodes/1/discard"))
            .andExpect(status().isOk)

        assert(savedArticles.size == 2) { "Expected 2 articles to be saved, got ${savedArticles.size}" }
        assert(savedArticles.all { !it.isProcessed }) { "Expected all articles to have isProcessed = false" }
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

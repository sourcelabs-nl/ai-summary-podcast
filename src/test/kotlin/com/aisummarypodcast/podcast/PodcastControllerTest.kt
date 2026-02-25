package com.aisummarypodcast.podcast

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.llm.LlmPipeline
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.User
import com.aisummarypodcast.user.UserService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(PodcastController::class)
class PodcastControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var podcastService: PodcastService

    @MockkBean
    private lateinit var userService: UserService

    @MockkBean
    private lateinit var llmPipeline: LlmPipeline

    @MockkBean
    private lateinit var episodeService: EpisodeService

    @MockkBean(relaxed = true)
    private lateinit var appProperties: AppProperties

    private val userId = "user-1"
    private val user = User(id = userId, name = "Test User")
    private val podcastId = "podcast-1"

    @Test
    fun `create podcast with relevanceThreshold and requireReview`() {
        val podcastSlot = slot<Podcast>()
        every { userService.findById(userId) } returns user
        every { podcastService.create(userId, "My Podcast", "tech", capture(podcastSlot)) } answers {
            podcastSlot.captured.copy(id = podcastId)
        }

        mockMvc.perform(
            post("/users/$userId/podcasts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"My Podcast","topic":"tech","relevanceThreshold":3,"requireReview":true}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.relevanceThreshold").value(3))
            .andExpect(jsonPath("$.requireReview").value(true))
    }

    @Test
    fun `update podcast with relevanceThreshold and requireReview`() {
        val existing = Podcast(
            id = podcastId, userId = userId, name = "My Podcast", topic = "tech",
            relevanceThreshold = 5, requireReview = false
        )
        val updatedSlot = slot<Podcast>()
        every { userService.findById(userId) } returns user
        every { podcastService.findById(podcastId) } returns existing
        every { podcastService.update(podcastId, capture(updatedSlot)) } answers {
            updatedSlot.captured
        }

        mockMvc.perform(
            put("/users/$userId/podcasts/$podcastId")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"My Podcast","topic":"tech","relevanceThreshold":8,"requireReview":true}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.relevanceThreshold").value(8))
            .andExpect(jsonPath("$.requireReview").value(true))
    }

    @Test
    fun `create podcast with all customization fields`() {
        val podcastSlot = slot<Podcast>()
        every { userService.findById(userId) } returns user
        every { podcastService.create(userId, "My Podcast", "tech", capture(podcastSlot)) } answers {
            podcastSlot.captured.copy(id = podcastId)
        }

        mockMvc.perform(
            post("/users/$userId/podcasts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"My Podcast","topic":"tech","ttsProvider":"openai","ttsVoices":{"default":"alloy"},"ttsSettings":{"speed":"1.25"},"style":"casual","targetWords":800,"relevanceThreshold":3,"requireReview":true,"cron":"0 0 8 * * MON-FRI"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.ttsProvider").value("openai"))
            .andExpect(jsonPath("$.ttsVoices.default").value("alloy"))
            .andExpect(jsonPath("$.style").value("casual"))
            .andExpect(jsonPath("$.targetWords").value(800))
            .andExpect(jsonPath("$.relevanceThreshold").value(3))
            .andExpect(jsonPath("$.requireReview").value(true))
            .andExpect(jsonPath("$.cron").value("0 0 8 * * MON-FRI"))
    }

    @Test
    fun `create interview podcast with valid config succeeds`() {
        val podcastSlot = slot<Podcast>()
        every { userService.findById(userId) } returns user
        every { podcastService.create(userId, "My Podcast", "tech", capture(podcastSlot)) } answers {
            podcastSlot.captured.copy(id = podcastId)
        }

        mockMvc.perform(
            post("/users/$userId/podcasts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"My Podcast","topic":"tech","style":"interview","ttsProvider":"elevenlabs","ttsVoices":{"interviewer":"v1","expert":"v2"},"speakerNames":{"interviewer":"Alice","expert":"Bob"}}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.style").value("interview"))
            .andExpect(jsonPath("$.speakerNames.interviewer").value("Alice"))
            .andExpect(jsonPath("$.speakerNames.expert").value("Bob"))
    }

    @Test
    fun `create interview podcast with wrong provider returns 400`() {
        every { userService.findById(userId) } returns user

        mockMvc.perform(
            post("/users/$userId/podcasts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"My Podcast","topic":"tech","style":"interview","ttsProvider":"openai","ttsVoices":{"interviewer":"v1","expert":"v2"}}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Interview style requires ElevenLabs or Inworld as TTS provider"))
    }

    @Test
    fun `create interview podcast with wrong role keys returns 400`() {
        every { userService.findById(userId) } returns user

        mockMvc.perform(
            post("/users/$userId/podcasts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"My Podcast","topic":"tech","style":"interview","ttsProvider":"elevenlabs","ttsVoices":{"host":"v1","cohost":"v2"}}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Interview style requires exactly 'interviewer' and 'expert' voice roles"))
    }

    @Test
    fun `create interview podcast with missing voices returns 400`() {
        every { userService.findById(userId) } returns user

        mockMvc.perform(
            post("/users/$userId/podcasts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"My Podcast","topic":"tech","style":"interview","ttsProvider":"elevenlabs","ttsVoices":{"interviewer":"v1"}}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Interview style requires at least two voice roles in ttsVoices (interviewer and expert)"))
    }

    @Test
    fun `create podcast without optional fields uses defaults`() {
        val podcastSlot = slot<Podcast>()
        every { userService.findById(userId) } returns user
        every { podcastService.create(userId, "My Podcast", "tech", capture(podcastSlot)) } answers {
            podcastSlot.captured.copy(id = podcastId)
        }

        mockMvc.perform(
            post("/users/$userId/podcasts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"My Podcast","topic":"tech"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.relevanceThreshold").value(5))
            .andExpect(jsonPath("$.requireReview").value(false))
            .andExpect(jsonPath("$.ttsProvider").value("openai"))
            .andExpect(jsonPath("$.style").value("news-briefing"))
    }
}

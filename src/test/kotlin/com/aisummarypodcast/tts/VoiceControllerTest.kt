package com.aisummarypodcast.tts

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.store.User
import com.aisummarypodcast.user.UserService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(VoiceController::class)
class VoiceControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var userService: UserService

    @MockkBean
    private lateinit var elevenLabsApiClient: ElevenLabsApiClient

    @MockkBean(relaxed = true)
    private lateinit var appProperties: AppProperties

    private val userId = "user-1"
    private val user = User(id = userId, name = "Test User")

    @Test
    fun `returns voices from ElevenLabs`() {
        every { userService.findById(userId) } returns user
        every { elevenLabsApiClient.listVoices(userId) } returns listOf(
            VoiceInfo("v1", "Rachel", "premade", "https://preview.com/rachel.mp3"),
            VoiceInfo("v2", "Adam", "cloned", null)
        )

        mockMvc.perform(get("/users/$userId/voices?provider=elevenlabs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].voiceId").value("v1"))
            .andExpect(jsonPath("$[0].name").value("Rachel"))
            .andExpect(jsonPath("$[0].category").value("premade"))
            .andExpect(jsonPath("$[0].previewUrl").value("https://preview.com/rachel.mp3"))
            .andExpect(jsonPath("$[1].voiceId").value("v2"))
            .andExpect(jsonPath("$[1].previewUrl").doesNotExist())
    }

    @Test
    fun `returns 404 when user not found`() {
        every { userService.findById(userId) } returns null

        mockMvc.perform(get("/users/$userId/voices?provider=elevenlabs"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `returns 400 for unsupported provider`() {
        every { userService.findById(userId) } returns user

        mockMvc.perform(get("/users/$userId/voices?provider=openai"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Voice discovery is only supported for the 'elevenlabs' provider"))
    }

    @Test
    fun `returns 400 when no ElevenLabs config`() {
        every { userService.findById(userId) } returns user
        every { elevenLabsApiClient.listVoices(userId) } throws IllegalStateException("No ElevenLabs provider config found. Configure an ElevenLabs API key for TTS.")

        mockMvc.perform(get("/users/$userId/voices?provider=elevenlabs"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("No ElevenLabs provider config found. Configure an ElevenLabs API key for TTS."))
    }

    @Test
    fun `returns 502 when ElevenLabs API fails`() {
        every { userService.findById(userId) } returns user
        every { elevenLabsApiClient.listVoices(userId) } throws RuntimeException("Connection refused")

        mockMvc.perform(get("/users/$userId/voices?provider=elevenlabs"))
            .andExpect(status().is5xxServerError)
            .andExpect(jsonPath("$.error").value("Failed to fetch voices from ElevenLabs: Connection refused"))
    }
}

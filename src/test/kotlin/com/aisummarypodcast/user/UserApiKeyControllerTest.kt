package com.aisummarypodcast.user

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.store.ApiKeyCategory
import com.aisummarypodcast.store.User
import com.aisummarypodcast.store.UserApiKey
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(UserApiKeyController::class)
class UserApiKeyControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var userApiKeyService: UserApiKeyService

    @MockkBean
    private lateinit var userService: UserService

    @MockkBean(relaxed = true)
    private lateinit var appProperties: AppProperties

    private val userId = "user-1"
    private val user = User(id = userId, name = "Test User")

    @Test
    fun `PUT with valid category sets key`() {
        every { userService.findById(userId) } returns user
        every { userApiKeyService.setKey(userId, ApiKeyCategory.LLM, "openrouter", "sk-123") } returns Unit

        mockMvc.perform(
            put("/users/$userId/api-keys/LLM")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"apiKey":"sk-123","provider":"openrouter"}""")
        ).andExpect(status().isOk)

        verify { userApiKeyService.setKey(userId, ApiKeyCategory.LLM, "openrouter", "sk-123") }
    }

    @Test
    fun `PUT with invalid category returns 400 with error message`() {
        mockMvc.perform(
            put("/users/$userId/api-keys/INVALID")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"apiKey":"sk-123","provider":"openrouter"}""")
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Invalid category. Must be one of: LLM, TTS"))
    }

    @Test
    fun `PUT with missing apiKey returns 400`() {
        every { userService.findById(userId) } returns user

        mockMvc.perform(
            put("/users/$userId/api-keys/LLM")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"apiKey":" ","provider":"openrouter"}""")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `PUT with missing provider returns 400`() {
        every { userService.findById(userId) } returns user

        mockMvc.perform(
            put("/users/$userId/api-keys/LLM")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"apiKey":"sk-123","provider":" "}""")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `PUT for non-existing user returns 404`() {
        every { userService.findById(userId) } returns null

        mockMvc.perform(
            put("/users/$userId/api-keys/LLM")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"apiKey":"sk-123","provider":"openrouter"}""")
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `GET lists keys with category and provider`() {
        every { userService.findById(userId) } returns user
        every { userApiKeyService.listKeys(userId) } returns listOf(
            UserApiKey(userId = userId, provider = "openrouter", category = ApiKeyCategory.LLM, encryptedApiKey = "enc"),
            UserApiKey(userId = userId, provider = "openai", category = ApiKeyCategory.TTS, encryptedApiKey = "enc")
        )

        mockMvc.perform(get("/users/$userId/api-keys"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].category").value("LLM"))
            .andExpect(jsonPath("$[0].provider").value("openrouter"))
            .andExpect(jsonPath("$[1].category").value("TTS"))
            .andExpect(jsonPath("$[1].provider").value("openai"))
    }

    @Test
    fun `GET returns empty array when no keys configured`() {
        every { userService.findById(userId) } returns user
        every { userApiKeyService.listKeys(userId) } returns emptyList()

        mockMvc.perform(get("/users/$userId/api-keys"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isEmpty)
    }

    @Test
    fun `GET for non-existing user returns 404`() {
        every { userService.findById(userId) } returns null

        mockMvc.perform(get("/users/$userId/api-keys"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE with valid category returns 204`() {
        every { userService.findById(userId) } returns user
        every { userApiKeyService.deleteKey(userId, ApiKeyCategory.LLM) } returns true

        mockMvc.perform(delete("/users/$userId/api-keys/LLM"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `DELETE with invalid category returns 400 with error message`() {
        mockMvc.perform(delete("/users/$userId/api-keys/INVALID"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Invalid category. Must be one of: LLM, TTS"))
    }

    @Test
    fun `DELETE non-existing key returns 404`() {
        every { userService.findById(userId) } returns user
        every { userApiKeyService.deleteKey(userId, ApiKeyCategory.TTS) } returns false

        mockMvc.perform(delete("/users/$userId/api-keys/TTS"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `PUT accepts lowercase category`() {
        every { userService.findById(userId) } returns user
        every { userApiKeyService.setKey(userId, ApiKeyCategory.LLM, "openrouter", "sk-123") } returns Unit

        mockMvc.perform(
            put("/users/$userId/api-keys/llm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"apiKey":"sk-123","provider":"openrouter"}""")
        ).andExpect(status().isOk)

        verify { userApiKeyService.setKey(userId, ApiKeyCategory.LLM, "openrouter", "sk-123") }
    }
}

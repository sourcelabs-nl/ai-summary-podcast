package com.aisummarypodcast.publishing

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.store.User
import com.aisummarypodcast.user.UserProviderConfigService
import com.aisummarypodcast.user.UserService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(PublishingTestController::class)
class PublishingTestControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var userService: UserService

    @MockkBean
    private lateinit var publishingTestService: PublishingTestService

    @MockkBean
    private lateinit var providerConfigService: UserProviderConfigService

    @MockkBean(relaxed = true)
    private lateinit var appProperties: AppProperties

    private val userId = "user-1"
    private val user = User(id = userId, name = "Test User")

    @Test
    fun `test FTP connection returns success`() {
        every { userService.findById(userId) } returns user
        every { publishingTestService.testFtp(any()) } returns TestConnectionResult(
            success = true, message = "Connected successfully"
        )

        mockMvc.perform(
            post("/users/$userId/publishing/test/ftp")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"host":"ftp.example.com","port":21,"username":"user","password":"pass","useTls":true}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Connected successfully"))
    }

    @Test
    fun `test FTP connection returns failure`() {
        every { userService.findById(userId) } returns user
        every { publishingTestService.testFtp(any()) } returns TestConnectionResult(
            success = false, message = "Authentication failed"
        )

        mockMvc.perform(
            post("/users/$userId/publishing/test/ftp")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"host":"ftp.example.com","port":21,"username":"user","password":"wrong","useTls":false}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.message").value("Authentication failed"))
    }

    @Test
    fun `test SoundCloud connection returns success with quota`() {
        every { userService.findById(userId) } returns user
        every { publishingTestService.testSoundCloud(userId) } returns TestConnectionResult(
            success = true,
            message = "Connected as testuser",
            quota = mapOf("uploadSecondsUsed" to 3600L, "uploadSecondsLeft" to 7200L)
        )

        mockMvc.perform(post("/users/$userId/publishing/test/soundcloud"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Connected as testuser"))
            .andExpect(jsonPath("$.quota.uploadSecondsUsed").value(3600))
    }

    @Test
    fun `test SoundCloud connection returns failure`() {
        every { userService.findById(userId) } returns user
        every { publishingTestService.testSoundCloud(userId) } returns TestConnectionResult(
            success = false, message = "No SoundCloud connection. Please authorize first."
        )

        mockMvc.perform(post("/users/$userId/publishing/test/soundcloud"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(false))
    }

    @Test
    fun `test unknown user returns 404`() {
        every { userService.findById(userId) } returns null

        mockMvc.perform(post("/users/$userId/publishing/test/soundcloud"))
            .andExpect(status().isNotFound)
    }
}

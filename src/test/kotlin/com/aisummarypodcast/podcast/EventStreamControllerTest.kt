package com.aisummarypodcast.podcast

import com.aisummarypodcast.config.AppProperties
import com.ninjasquad.springmockk.MockkBean
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@WebMvcTest(EventStreamController::class)
class EventStreamControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean(relaxed = true)
    private lateinit var broadcaster: SseEventBroadcaster

    @MockkBean(relaxed = true)
    private lateinit var appProperties: AppProperties

    @Test
    fun `event stream endpoint returns SSE stream and registers emitter`() {
        mockMvc.perform(get("/users/u1/events").accept("text/event-stream"))
            .andExpect(status().isOk)
            .andExpect(request().asyncStarted())

        verify { broadcaster.register("u1", any<SseEmitter>()) }
    }
}

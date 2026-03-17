package com.aisummarypodcast.podcast

import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PodcastRepository
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.Optional

class SseEventBroadcasterTest {

    private val podcastRepository = mockk<PodcastRepository>()
    private val objectMapper = jacksonObjectMapper()
    private val broadcaster = SseEventBroadcaster(podcastRepository, objectMapper)

    private val podcast = Podcast(id = "p1", userId = "u1", name = "Test", topic = "tech")

    @Test
    fun `broadcasts event to registered emitter`() {
        every { podcastRepository.findById("p1") } returns Optional.of(podcast)
        val emitter = mockk<SseEmitter>(relaxed = true)
        broadcaster.register("u1", emitter)

        val event = PodcastEvent(this, "p1", "episode", 42L, "episode.generated")
        broadcaster.onPodcastEvent(event)

        io.mockk.verify { emitter.send(any<SseEmitter.SseEventBuilder>()) }
    }

    @Test
    fun `broadcasts to multiple emitters for same user`() {
        every { podcastRepository.findById("p1") } returns Optional.of(podcast)
        val emitter1 = mockk<SseEmitter>(relaxed = true)
        val emitter2 = mockk<SseEmitter>(relaxed = true)
        broadcaster.register("u1", emitter1)
        broadcaster.register("u1", emitter2)

        val event = PodcastEvent(this, "p1", "episode", 42L, "episode.generated")
        broadcaster.onPodcastEvent(event)

        io.mockk.verify { emitter1.send(any<SseEmitter.SseEventBuilder>()) }
        io.mockk.verify { emitter2.send(any<SseEmitter.SseEventBuilder>()) }
    }

    @Test
    fun `removes dead emitters on send failure`() {
        every { podcastRepository.findById("p1") } returns Optional.of(podcast)
        val deadEmitter = mockk<SseEmitter> {
            every { send(any<SseEmitter.SseEventBuilder>()) } throws Exception("Connection closed")
        }
        broadcaster.register("u1", deadEmitter)

        val event = PodcastEvent(this, "p1", "episode", 42L, "episode.generated")
        broadcaster.onPodcastEvent(event)

        // Second event should not attempt to send to dead emitter
        broadcaster.onPodcastEvent(event)
        io.mockk.verify(exactly = 1) { deadEmitter.send(any<SseEmitter.SseEventBuilder>()) }
    }

    @Test
    fun `no-op when no emitters registered`() {
        every { podcastRepository.findById("p1") } returns Optional.of(podcast)
        val event = PodcastEvent(this, "p1", "episode", 42L, "episode.generated")
        // Should not throw
        broadcaster.onPodcastEvent(event)
    }

    @Test
    fun `no-op when podcast not found`() {
        every { podcastRepository.findById("unknown") } returns Optional.empty()
        val event = PodcastEvent(this, "unknown", "episode", 42L, "episode.generated")
        // Should not throw
        broadcaster.onPodcastEvent(event)
    }

    @Test
    fun `sendHeartbeat sends to all emitters`() {
        val emitter = mockk<SseEmitter>(relaxed = true)
        broadcaster.register("u1", emitter)

        broadcaster.sendHeartbeat()

        io.mockk.verify { emitter.send(any<SseEmitter.SseEventBuilder>()) }
    }

    @Test
    fun `sendHeartbeat removes dead emitters`() {
        val deadEmitter = mockk<SseEmitter> {
            every { send(any<SseEmitter.SseEventBuilder>()) } throws Exception("Connection closed")
        }
        broadcaster.register("u1", deadEmitter)

        broadcaster.sendHeartbeat()

        // Emitter should be removed, second heartbeat should not attempt to send
        broadcaster.sendHeartbeat()
        io.mockk.verify(exactly = 1) { deadEmitter.send(any<SseEmitter.SseEventBuilder>()) }
    }

    @Test
    fun `remove deregisters emitter`() {
        every { podcastRepository.findById("p1") } returns Optional.of(podcast)
        val emitter = mockk<SseEmitter>(relaxed = true)
        broadcaster.register("u1", emitter)
        broadcaster.remove("u1", emitter)

        val event = PodcastEvent(this, "p1", "episode", 42L, "episode.generated")
        broadcaster.onPodcastEvent(event)

        io.mockk.verify(exactly = 0) { emitter.send(any<SseEmitter.SseEventBuilder>()) }
    }
}

package com.aisummarypodcast.podcast

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
class EventStreamController(
    private val broadcaster: SseEventBroadcaster
) {

    @GetMapping("/users/{userId}/events", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun eventStream(@PathVariable userId: String): SseEmitter {
        val emitter = SseEmitter(86_400_000L) // 24 hours

        broadcaster.register(userId, emitter)

        emitter.onCompletion { broadcaster.remove(userId, emitter) }
        emitter.onTimeout { broadcaster.remove(userId, emitter) }
        emitter.onError { broadcaster.remove(userId, emitter) }

        return emitter
    }
}

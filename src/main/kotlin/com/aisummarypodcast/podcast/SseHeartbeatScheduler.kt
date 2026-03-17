package com.aisummarypodcast.podcast

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class SseHeartbeatScheduler(
    private val broadcaster: SseEventBroadcaster
) {

    @Scheduled(fixedRate = 30_000)
    fun sendHeartbeat() {
        broadcaster.sendHeartbeat()
    }
}

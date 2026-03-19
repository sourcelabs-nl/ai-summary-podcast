package com.aisummarypodcast.podcast

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class PipelineStateTracker {

    private val activeStages = ConcurrentHashMap<String, String>()

    @EventListener
    fun onPodcastEvent(event: PodcastEvent) {
        when (event.event) {
            "pipeline.progress" -> {
                val stage = event.data["stage"] as? String ?: return
                activeStages[event.podcastId] = stage
            }
            "episode.created", "episode.generated", "episode.failed" -> {
                activeStages.remove(event.podcastId)
            }
        }
    }

    fun getStage(podcastId: String): String? = activeStages[podcastId]

    fun clear(podcastId: String) {
        activeStages.remove(podcastId)
    }
}

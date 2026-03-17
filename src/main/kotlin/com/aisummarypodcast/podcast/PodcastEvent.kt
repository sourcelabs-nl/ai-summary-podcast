package com.aisummarypodcast.podcast

import org.springframework.context.ApplicationEvent

class PodcastEvent(
    source: Any,
    val podcastId: String,
    val entityType: String,
    val entityId: Long,
    val event: String,
    val data: Map<String, Any> = emptyMap()
) : ApplicationEvent(source)

package com.aisummarypodcast.publishing

import org.springframework.stereotype.Component

@Component
class PublisherRegistry(publishers: List<EpisodePublisher>) {

    private val publishersByTarget: Map<String, EpisodePublisher> =
        publishers.associateBy { it.targetName() }

    fun getPublisher(target: String): EpisodePublisher? = publishersByTarget[target]
}

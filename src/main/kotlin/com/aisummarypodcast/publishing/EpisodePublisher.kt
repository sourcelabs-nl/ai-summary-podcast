package com.aisummarypodcast.publishing

import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.Podcast

data class PublishResult(
    val externalId: String,
    val externalUrl: String
)

interface EpisodePublisher {

    fun targetName(): String

    fun publish(episode: Episode, podcast: Podcast, userId: String): PublishResult
}

package com.aisummarypodcast.store

import org.springframework.data.repository.CrudRepository

interface PodcastPublicationTargetRepository : CrudRepository<PodcastPublicationTarget, Long>, PodcastPublicationTargetRepositoryCustom {

    fun findByPodcastId(podcastId: String): List<PodcastPublicationTarget>

    fun findByPodcastIdAndTarget(podcastId: String, target: String): PodcastPublicationTarget?

    fun deleteByPodcastIdAndTarget(podcastId: String, target: String): Long
}

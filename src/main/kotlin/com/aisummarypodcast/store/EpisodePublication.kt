package com.aisummarypodcast.store

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("episode_publications")
data class EpisodePublication(
    @Id val id: Long? = null,
    val episodeId: Long,
    val target: String,
    val status: PublicationStatus,
    val externalId: String? = null,
    val externalUrl: String? = null,
    val errorMessage: String? = null,
    val publishedAt: String? = null,
    val createdAt: String
)

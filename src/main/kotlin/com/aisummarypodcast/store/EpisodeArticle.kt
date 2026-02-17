package com.aisummarypodcast.store

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("episode_articles")
data class EpisodeArticle(
    @Id val id: Long? = null,
    val episodeId: Long,
    val articleId: Long
)

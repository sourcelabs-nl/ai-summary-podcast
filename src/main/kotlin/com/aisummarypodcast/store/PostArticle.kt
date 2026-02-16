package com.aisummarypodcast.store

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("post_articles")
data class PostArticle(
    @Id val id: Long? = null,
    val postId: Long,
    val articleId: Long
)

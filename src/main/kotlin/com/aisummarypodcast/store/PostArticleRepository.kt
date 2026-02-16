package com.aisummarypodcast.store

import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository

interface PostArticleRepository : CrudRepository<PostArticle, Long> {

    @Query("SELECT * FROM post_articles WHERE article_id = :articleId")
    fun findByArticleId(articleId: Long): List<PostArticle>
}

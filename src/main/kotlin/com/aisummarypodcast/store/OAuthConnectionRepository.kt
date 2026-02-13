package com.aisummarypodcast.store

import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository

interface OAuthConnectionRepository : CrudRepository<OAuthConnection, Long> {

    @Query("SELECT * FROM oauth_connections WHERE user_id = :userId AND provider = :provider")
    fun findByUserIdAndProvider(userId: String, provider: String): OAuthConnection?

    @Modifying
    @Query("DELETE FROM oauth_connections WHERE user_id = :userId AND provider = :provider")
    fun deleteByUserIdAndProvider(userId: String, provider: String): Int

    @Modifying
    @Query("DELETE FROM oauth_connections WHERE user_id = :userId")
    fun deleteByUserId(userId: String)
}

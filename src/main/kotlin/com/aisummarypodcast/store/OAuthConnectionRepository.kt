package com.aisummarypodcast.store

import org.springframework.data.repository.CrudRepository

interface OAuthConnectionRepository : CrudRepository<OAuthConnection, Long> {

    fun findByUserIdAndProvider(userId: String, provider: String): OAuthConnection?

    fun deleteByUserIdAndProvider(userId: String, provider: String): Long

    fun deleteByUserId(userId: String)
}

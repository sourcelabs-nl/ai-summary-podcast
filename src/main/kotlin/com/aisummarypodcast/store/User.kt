package com.aisummarypodcast.store

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Table

@Table("users")
data class User(
    @Id val id: String,
    val name: String,
    @Version val version: Long? = null
)

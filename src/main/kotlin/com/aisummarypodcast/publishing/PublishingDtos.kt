package com.aisummarypodcast.publishing

data class PublicationResponse(
    val id: Long,
    val episodeId: Long,
    val target: String,
    val status: String,
    val externalId: String?,
    val externalUrl: String?,
    val errorMessage: String?,
    val publishedAt: String?,
    val createdAt: String
)

data class PublicationTargetRequest(
    val config: Map<String, Any>? = null,
    val enabled: Boolean = false
)

data class PublicationTargetResponse(
    val target: String,
    val config: Map<String, Any>,
    val enabled: Boolean
)

data class AuthorizeResponse(val authorizationUrl: String)

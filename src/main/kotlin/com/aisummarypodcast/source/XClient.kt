package com.aisummarypodcast.source

import org.slf4j.LoggerFactory
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class XTokenResponse(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresIn: Long? = null,
    val tokenType: String? = null,
    val scope: String? = null
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class XUserData(
    val id: String,
    val username: String,
    val name: String? = null
)

data class XUserResponse(
    val data: XUserData? = null
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class XTweet(
    val id: String,
    val text: String,
    val createdAt: String? = null,
    val authorId: String? = null
)

data class XTweetsResponse(
    val data: List<XTweet>? = null
)

@Component
class XClient(restTemplateBuilder: RestTemplateBuilder) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val restTemplate: RestTemplate = restTemplateBuilder.build()

    fun exchangeCodeForTokens(
        code: String,
        clientId: String,
        clientSecret: String,
        redirectUri: String,
        codeVerifier: String
    ): XTokenResponse {
        val body = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "authorization_code")
            add("code", code)
            add("redirect_uri", redirectUri)
            add("code_verifier", codeVerifier)
        }

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            setBasicAuth(clientId, clientSecret)
        }

        log.info("Exchanging authorization code for X tokens")
        val response = restTemplate.postForEntity(
            "https://api.x.com/2/oauth2/token",
            HttpEntity(body, headers),
            XTokenResponse::class.java
        )
        return response.body ?: throw RuntimeException("Empty response from X token exchange")
    }

    fun refreshAccessToken(
        refreshToken: String,
        clientId: String,
        clientSecret: String
    ): XTokenResponse {
        val body = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "refresh_token")
            add("refresh_token", refreshToken)
        }

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
            setBasicAuth(clientId, clientSecret)
        }

        log.info("Refreshing X access token")
        val response = restTemplate.postForEntity(
            "https://api.x.com/2/oauth2/token",
            HttpEntity(body, headers),
            XTokenResponse::class.java
        )
        return response.body ?: throw RuntimeException("Empty response from X token refresh")
    }

    fun resolveUsername(accessToken: String, username: String): XUserData {
        val headers = HttpHeaders().apply {
            setBearerAuth(accessToken)
        }

        log.info("Resolving X username: {}", username)
        val response = restTemplate.exchange(
            "https://api.x.com/2/users/by/username/$username",
            HttpMethod.GET,
            HttpEntity<Any>(headers),
            XUserResponse::class.java
        )
        return response.body?.data
            ?: throw RuntimeException("X user not found: $username")
    }

    fun getUserTimeline(
        accessToken: String,
        userId: String,
        sinceId: String? = null
    ): List<XTweet> {
        val headers = HttpHeaders().apply {
            setBearerAuth(accessToken)
        }

        var url = "https://api.x.com/2/users/$userId/tweets?tweet.fields=text,created_at,author_id"
        if (sinceId != null) {
            url += "&since_id=$sinceId"
        }

        log.info("Fetching X timeline for user {}", userId)
        val response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            HttpEntity<Any>(headers),
            XTweetsResponse::class.java
        )
        return response.body?.data ?: emptyList()
    }

    fun isRateLimited(e: HttpClientErrorException): Boolean = e.statusCode.value() == 429

    fun isAuthError(e: HttpClientErrorException): Boolean =
        e.statusCode.value() == 401 || e.statusCode.value() == 403

    fun isNotFound(e: HttpClientErrorException): Boolean = e.statusCode.value() == 404
}

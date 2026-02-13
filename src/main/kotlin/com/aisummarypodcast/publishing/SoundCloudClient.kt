package com.aisummarypodcast.publishing

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestTemplate
import java.nio.file.Path

data class SoundCloudTokenResponse(
    @JsonProperty("access_token") val accessToken: String,
    @JsonProperty("refresh_token") val refreshToken: String? = null,
    @JsonProperty("expires_in") val expiresIn: Long? = null,
    @JsonProperty("token_type") val tokenType: String? = null,
    @JsonProperty("scope") val scope: String? = null
)

data class SoundCloudTrackResponse(
    val id: Long,
    @JsonProperty("permalink_url") val permalinkUrl: String,
    val title: String? = null
)

data class TrackUploadRequest(
    val title: String,
    val description: String,
    val tagList: String,
    val audioFilePath: Path
)

@Service
class SoundCloudClient(private val objectMapper: ObjectMapper) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val restTemplate = RestTemplate()

    fun exchangeCodeForTokens(
        code: String,
        clientId: String,
        clientSecret: String,
        redirectUri: String,
        codeVerifier: String
    ): SoundCloudTokenResponse {
        val body = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "authorization_code")
            add("client_id", clientId)
            add("client_secret", clientSecret)
            add("redirect_uri", redirectUri)
            add("code", code)
            add("code_verifier", codeVerifier)
        }

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
        }

        log.info("Exchanging authorization code for SoundCloud tokens")
        val response = restTemplate.postForEntity(
            "https://api.soundcloud.com/oauth2/token",
            HttpEntity(body, headers),
            SoundCloudTokenResponse::class.java
        )
        return response.body ?: throw RuntimeException("Empty response from SoundCloud token exchange")
    }

    fun refreshAccessToken(
        refreshToken: String,
        clientId: String,
        clientSecret: String
    ): SoundCloudTokenResponse {
        val body = LinkedMultiValueMap<String, String>().apply {
            add("grant_type", "refresh_token")
            add("client_id", clientId)
            add("client_secret", clientSecret)
            add("refresh_token", refreshToken)
        }

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_FORM_URLENCODED
        }

        log.info("Refreshing SoundCloud access token")
        val response = restTemplate.postForEntity(
            "https://api.soundcloud.com/oauth2/token",
            HttpEntity(body, headers),
            SoundCloudTokenResponse::class.java
        )
        return response.body ?: throw RuntimeException("Empty response from SoundCloud token refresh")
    }

    fun uploadTrack(
        accessToken: String,
        request: TrackUploadRequest
    ): SoundCloudTrackResponse {
        val body = LinkedMultiValueMap<String, Any>().apply {
            add("track[title]", request.title)
            add("track[description]", request.description)
            add("track[tag_list]", request.tagList)
            add("track[sharing]", "public")
            add("track[asset_data]", FileSystemResource(request.audioFilePath))
        }

        val headers = HttpHeaders().apply {
            contentType = MediaType.MULTIPART_FORM_DATA
            setBearerAuth(accessToken)
        }

        log.info("Uploading track to SoundCloud: {}", request.title)
        val response = restTemplate.exchange(
            "https://api.soundcloud.com/tracks",
            HttpMethod.POST,
            HttpEntity(body, headers),
            SoundCloudTrackResponse::class.java
        )
        return response.body ?: throw RuntimeException("Empty response from SoundCloud track upload")
    }
}

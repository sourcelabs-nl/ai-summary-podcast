package com.aisummarypodcast.publishing

import org.slf4j.LoggerFactory
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.web.client.RestTemplate
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming
import java.nio.file.Path

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SoundCloudTokenResponse(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresIn: Long? = null,
    val tokenType: String? = null,
    val scope: String? = null
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SoundCloudTrackResponse(
    val id: Long,
    val permalinkUrl: String,
    val title: String? = null
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SoundCloudPlaylistResponse(
    val id: Long,
    val permalinkUrl: String? = null
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SoundCloudPlaylistDetailResponse(
    val id: Long,
    val tracks: List<SoundCloudPlaylistTrack> = emptyList()
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class SoundCloudPlaylistTrack(
    val id: Long
)

data class TrackUploadRequest(
    val title: String,
    val description: String,
    val tagList: String,
    val audioFilePath: Path
)

@Service
class SoundCloudClient(restTemplateBuilder: RestTemplateBuilder) {

    private val log = LoggerFactory.getLogger(javaClass)
    private val restTemplate: RestTemplate = restTemplateBuilder.build()

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
            "https://secure.soundcloud.com/oauth/token",
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
            "https://secure.soundcloud.com/oauth/token",
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

    fun createPlaylist(
        accessToken: String,
        title: String,
        trackIds: List<Long>
    ): SoundCloudPlaylistResponse {
        val body = mapOf(
            "playlist" to mapOf(
                "title" to title,
                "sharing" to "public",
                "tracks" to trackIds.map { mapOf("id" to it.toString()) }
            )
        )

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth(accessToken)
        }

        log.info("Creating SoundCloud playlist: {}", title)
        val response = restTemplate.postForEntity(
            "https://api.soundcloud.com/playlists",
            HttpEntity(body, headers),
            SoundCloudPlaylistResponse::class.java
        )
        return response.body ?: throw RuntimeException("Empty response from SoundCloud playlist creation")
    }

    fun getPlaylist(
        accessToken: String,
        playlistId: Long
    ): SoundCloudPlaylistDetailResponse {
        val headers = HttpHeaders().apply {
            setBearerAuth(accessToken)
        }

        log.info("Fetching SoundCloud playlist {}", playlistId)
        val response = restTemplate.exchange(
            "https://api.soundcloud.com/playlists/$playlistId",
            HttpMethod.GET,
            HttpEntity<Any>(headers),
            SoundCloudPlaylistDetailResponse::class.java
        )
        return response.body ?: throw RuntimeException("Empty response from SoundCloud playlist fetch")
    }

    fun addTrackToPlaylist(
        accessToken: String,
        playlistId: Long,
        trackIds: List<Long>
    ): SoundCloudPlaylistResponse {
        val body = mapOf(
            "playlist" to mapOf(
                "tracks" to trackIds.map { mapOf("id" to it.toString()) }
            )
        )

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            setBearerAuth(accessToken)
        }

        log.info("Adding tracks to SoundCloud playlist {}", playlistId)
        val response = restTemplate.exchange(
            "https://api.soundcloud.com/playlists/$playlistId",
            HttpMethod.PUT,
            HttpEntity(body, headers),
            SoundCloudPlaylistResponse::class.java
        )
        return response.body ?: throw RuntimeException("Empty response from SoundCloud playlist update")
    }
}

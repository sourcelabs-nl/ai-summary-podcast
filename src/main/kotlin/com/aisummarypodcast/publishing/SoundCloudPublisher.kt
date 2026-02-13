package com.aisummarypodcast.publishing

import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PodcastRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import java.nio.file.Path
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Component
class SoundCloudPublisher(
    private val soundCloudClient: SoundCloudClient,
    private val tokenManager: SoundCloudTokenManager,
    private val podcastRepository: PodcastRepository
) : EpisodePublisher {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun targetName(): String = "soundcloud"

    override fun publish(episode: Episode, podcast: Podcast, userId: String): PublishResult {
        val accessToken = tokenManager.getValidAccessToken(userId)

        val episodeDate = LocalDate.parse(
            episode.generatedAt,
            DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC)
        )
        val title = "${podcast.name} - $episodeDate"
        val description = episode.scriptText.take(500)
        val tagList = buildTagList(podcast.topic)

        val response = soundCloudClient.uploadTrack(
            accessToken = accessToken,
            request = TrackUploadRequest(
                title = title,
                description = description,
                tagList = tagList,
                audioFilePath = Path.of(episode.audioFilePath!!)
            )
        )

        addToPlaylist(accessToken, podcast, response.id)

        return PublishResult(
            externalId = response.id.toString(),
            externalUrl = response.permalinkUrl
        )
    }

    private fun addToPlaylist(accessToken: String, podcast: Podcast, trackId: Long) {
        val playlistId = podcast.soundcloudPlaylistId?.toLongOrNull()

        if (playlistId == null) {
            createNewPlaylist(accessToken, podcast, trackId)
            return
        }

        try {
            soundCloudClient.addTrackToPlaylist(accessToken, playlistId, listOf(trackId))
            log.info("Added track {} to SoundCloud playlist {}", trackId, playlistId)
        } catch (e: HttpClientErrorException.NotFound) {
            log.warn("SoundCloud playlist {} not found, creating new playlist", playlistId)
            createNewPlaylist(accessToken, podcast, trackId)
        }
    }

    private fun createNewPlaylist(accessToken: String, podcast: Podcast, trackId: Long) {
        val playlist = soundCloudClient.createPlaylist(accessToken, podcast.name, listOf(trackId))
        podcastRepository.save(podcast.copy(soundcloudPlaylistId = playlist.id.toString()))
        log.info("Created SoundCloud playlist {} for podcast {}", playlist.id, podcast.id)
    }

    private fun buildTagList(topic: String): String {
        return topic.split(",", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString(" ") { tag ->
                if (tag.contains(" ")) "\"$tag\"" else tag
            }
    }
}

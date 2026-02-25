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
        val permalink = buildPermalink(podcast.name, episodeDate)
        val description = episode.recap ?: episode.scriptText.take(500)
        val tagList = buildTagList(podcast.topic)

        val response = soundCloudClient.uploadTrack(
            accessToken = accessToken,
            request = TrackUploadRequest(
                title = title,
                description = description,
                tagList = tagList,
                permalink = permalink,
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
            val existing = soundCloudClient.getPlaylist(accessToken, playlistId)
            val allTrackIds = existing.tracks.map { it.id } + trackId
            soundCloudClient.addTrackToPlaylist(accessToken, playlistId, allTrackIds)
            log.info("Added track {} to SoundCloud playlist {} (now {} tracks)", trackId, playlistId, allTrackIds.size)
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

    fun updateTrackPermalinks(podcast: Podcast, userId: String, episodes: List<Episode>, publications: List<com.aisummarypodcast.store.EpisodePublication>) {
        val accessToken = tokenManager.getValidAccessToken(userId)
        val episodeById = episodes.associateBy { it.id }

        for (publication in publications) {
            val episode = episodeById[publication.episodeId] ?: continue
            val trackId = publication.externalId?.toLongOrNull() ?: continue
            val episodeDate = LocalDate.parse(
                episode.generatedAt,
                DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC)
            )
            val permalink = buildPermalink(podcast.name, episodeDate)
            val description = episode.recap ?: episode.scriptText.take(500)
            soundCloudClient.updateTrack(accessToken, trackId, permalink = permalink, description = description)
        }
    }

    fun rebuildPlaylist(podcast: Podcast, userId: String, trackIds: List<Long>) {
        val accessToken = tokenManager.getValidAccessToken(userId)
        val playlistId = podcast.soundcloudPlaylistId?.toLongOrNull()

        if (playlistId == null) {
            val playlist = soundCloudClient.createPlaylist(accessToken, podcast.name, trackIds)
            podcastRepository.save(podcast.copy(soundcloudPlaylistId = playlist.id.toString()))
            log.info("Created SoundCloud playlist {} with {} tracks for podcast {}", playlist.id, trackIds.size, podcast.id)
            return
        }

        try {
            soundCloudClient.addTrackToPlaylist(accessToken, playlistId, trackIds)
            log.info("Rebuilt SoundCloud playlist {} with {} tracks", playlistId, trackIds.size)
        } catch (e: HttpClientErrorException.NotFound) {
            log.warn("SoundCloud playlist {} not found, creating new playlist", playlistId)
            val playlist = soundCloudClient.createPlaylist(accessToken, podcast.name, trackIds)
            podcastRepository.save(podcast.copy(soundcloudPlaylistId = playlist.id.toString()))
            log.info("Created SoundCloud playlist {} with {} tracks for podcast {}", playlist.id, trackIds.size, podcast.id)
        }
    }

    private fun buildPermalink(podcastName: String, episodeDate: LocalDate): String {
        return "${podcastName}-${episodeDate}"
            .lowercase()
            .replace(Regex("[^a-z0-9-]"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
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

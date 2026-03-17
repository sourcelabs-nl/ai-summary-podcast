package com.aisummarypodcast.publishing

import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.Podcast
import tools.jackson.databind.ObjectMapper
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
    private val targetService: PodcastPublicationTargetService,
    private val objectMapper: ObjectMapper
) : EpisodePublisher {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun targetName(): String = "soundcloud"

    override fun publish(episode: Episode, podcast: Podcast, userId: String): PublishResult {
        val accessToken = tokenManager.getValidAccessToken(userId)

        val me = soundCloudClient.getMe(accessToken)
        val quota = me.quota
        if (quota != null && !quota.unlimitedUploadQuota && quota.uploadSecondsLeft <= 0) {
            val oldestTrack = try {
                soundCloudClient.getMyTracks(accessToken).collection
                    .filter { it.title?.startsWith(podcast.name) == true }
                    .minByOrNull { it.createdAt ?: "" }
            } catch (e: Exception) {
                log.warn("Failed to fetch tracks for oldest track lookup: {}", e.message)
                null
            }
            throw SoundCloudQuotaExceededException(
                message = "SoundCloud upload quota exceeded. " +
                    "Used: ${formatDuration(quota.uploadSecondsUsed)}, " +
                    "over by: ${formatDuration(-quota.uploadSecondsLeft)}. " +
                    "Delete an existing track or upgrade your plan.",
                oldestTrack = oldestTrack
            )
        }

        val episodeDate = LocalDate.parse(
            episode.generatedAt,
            DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC)
        )
        val title = "${podcast.name} - $episodeDate"
        val permalink = buildPermalink(podcast.name, episodeDate)
        val description = episode.showNotes ?: episode.recap ?: episode.scriptText.take(500)
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

    override fun unpublish(userId: String, externalId: String) {
        val accessToken = tokenManager.getValidAccessToken(userId)
        val trackId = externalId.toLong()
        soundCloudClient.deleteTrack(accessToken, trackId)
        log.info("Unpublished (deleted) SoundCloud track {}", trackId)
    }

    override fun update(episode: Episode, podcast: Podcast, userId: String, externalId: String): PublishResult {
        val accessToken = tokenManager.getValidAccessToken(userId)
        val trackId = externalId.toLong()
        val description = episode.showNotes ?: episode.recap ?: episode.scriptText.take(500)
        val response = soundCloudClient.updateTrack(accessToken, trackId, description = description)
        log.info("Updated SoundCloud track {} description for episode {}", trackId, episode.id)
        return PublishResult(
            externalId = externalId,
            externalUrl = response.permalinkUrl
        )
    }

    private fun getPlaylistId(podcastId: String): Long? {
        val target = targetService.get(podcastId, "soundcloud") ?: return null
        val config = objectMapper.readTree(target.config)
        return config.get("playlistId")?.asText()?.toLongOrNull()
    }

    private fun savePlaylistId(podcastId: String, playlistId: Long) {
        val target = targetService.get(podcastId, "soundcloud")
        val config = target?.config?.let { objectMapper.readTree(it) }
            ?.let { (it as tools.jackson.databind.node.ObjectNode).put("playlistId", playlistId.toString()) }
            ?: objectMapper.createObjectNode().put("playlistId", playlistId.toString())
        targetService.upsert(podcastId, "soundcloud", objectMapper.writeValueAsString(config), target?.enabled ?: true)
    }

    private fun addToPlaylist(accessToken: String, podcast: Podcast, trackId: Long) {
        val playlistId = getPlaylistId(podcast.id)

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
        savePlaylistId(podcast.id, playlist.id)
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
            val description = episode.showNotes ?: episode.recap ?: episode.scriptText.take(500)
            soundCloudClient.updateTrack(accessToken, trackId, permalink = permalink, description = description)
        }
    }

    fun rebuildPlaylist(podcast: Podcast, userId: String, trackIds: List<Long>) {
        val accessToken = tokenManager.getValidAccessToken(userId)
        val playlistId = getPlaylistId(podcast.id)

        if (playlistId == null) {
            val playlist = soundCloudClient.createPlaylist(accessToken, podcast.name, trackIds)
            savePlaylistId(podcast.id, playlist.id)
            log.info("Created SoundCloud playlist {} with {} tracks for podcast {}", playlist.id, trackIds.size, podcast.id)
            return
        }

        try {
            soundCloudClient.addTrackToPlaylist(accessToken, playlistId, trackIds)
            log.info("Rebuilt SoundCloud playlist {} with {} tracks", playlistId, trackIds.size)
        } catch (e: HttpClientErrorException.NotFound) {
            log.warn("SoundCloud playlist {} not found, creating new playlist", playlistId)
            val playlist = soundCloudClient.createPlaylist(accessToken, podcast.name, trackIds)
            savePlaylistId(podcast.id, playlist.id)
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

    private fun formatDuration(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}

class SoundCloudQuotaExceededException(
    message: String,
    val oldestTrack: SoundCloudTrackSummary? = null
) : RuntimeException(message)

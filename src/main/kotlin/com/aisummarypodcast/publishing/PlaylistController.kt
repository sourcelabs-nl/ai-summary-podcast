package com.aisummarypodcast.publishing

import com.aisummarypodcast.podcast.PodcastService
import com.aisummarypodcast.store.EpisodePublicationRepository
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.user.UserService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users/{userId}/podcasts/{podcastId}/playlist")
class PlaylistController(
    private val userService: UserService,
    private val podcastService: PodcastService,
    private val episodeRepository: EpisodeRepository,
    private val episodePublicationRepository: EpisodePublicationRepository,
    private val soundCloudPublisher: SoundCloudPublisher
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/rebuild")
    fun rebuildPlaylist(
        @PathVariable userId: String,
        @PathVariable podcastId: String
    ): ResponseEntity<Any> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val podcast = podcastService.findById(podcastId) ?: return ResponseEntity.notFound().build()
        if (podcast.userId != userId) return ResponseEntity.notFound().build()

        val publications = episodePublicationRepository.findPublishedByPodcastIdAndTarget(podcastId, "soundcloud")
        if (publications.isEmpty()) {
            return ResponseEntity.badRequest().body(mapOf("error" to "No published SoundCloud tracks found for this podcast"))
        }

        val trackIds = publications.mapNotNull { it.externalId?.toLongOrNull() }
        if (trackIds.isEmpty()) {
            return ResponseEntity.badRequest().body(mapOf("error" to "No valid SoundCloud track IDs found"))
        }

        return try {
            val episodeIds = publications.map { it.episodeId }.distinct()
            val episodes = episodeIds.mapNotNull { episodeRepository.findById(it).orElse(null) }
            soundCloudPublisher.updateTrackPermalinks(podcast, userId, episodes, publications)
            soundCloudPublisher.rebuildPlaylist(podcast, userId, trackIds)
            log.info("Updated permalinks and rebuilt SoundCloud playlist for podcast {} with {} tracks", podcastId, trackIds.size)
            ResponseEntity.ok(mapOf("trackIds" to trackIds))
        } catch (e: Exception) {
            log.error("Failed to rebuild playlist for podcast {}: {}", podcastId, e.message, e)
            ResponseEntity.internalServerError().body(mapOf("error" to "Failed to rebuild playlist: ${e.message}"))
        }
    }
}

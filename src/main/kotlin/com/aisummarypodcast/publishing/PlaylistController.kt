package com.aisummarypodcast.publishing

import com.aisummarypodcast.podcast.PodcastService
import com.aisummarypodcast.user.UserService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users/{userId}/podcasts/{podcastId}/playlist")
class PlaylistController(
    private val userService: UserService,
    private val podcastService: PodcastService,
    private val publishingService: PublishingService
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

        return try {
            val trackIds = publishingService.rebuildSoundCloudPlaylist(podcast, userId)
            ResponseEntity.ok(mapOf("trackIds" to trackIds))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: Exception) {
            log.error("Failed to rebuild playlist for podcast {}: {}", podcastId, e.message, e)
            ResponseEntity.internalServerError().body(mapOf("error" to "Failed to rebuild playlist: ${e.message}"))
        }
    }
}

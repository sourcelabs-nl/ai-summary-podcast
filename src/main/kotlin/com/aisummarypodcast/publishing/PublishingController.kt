package com.aisummarypodcast.publishing

import com.aisummarypodcast.podcast.PodcastService
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.user.UserService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

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

@RestController
@RequestMapping("/users/{userId}/podcasts/{podcastId}/episodes/{episodeId}")
class PublishingController(
    private val userService: UserService,
    private val podcastService: PodcastService,
    private val episodeRepository: EpisodeRepository,
    private val publishingService: PublishingService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/publish/{target}")
    fun publish(
        @PathVariable userId: String,
        @PathVariable podcastId: String,
        @PathVariable episodeId: Long,
        @PathVariable target: String
    ): ResponseEntity<Any> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val podcast = podcastService.findById(podcastId) ?: return ResponseEntity.notFound().build()
        if (podcast.userId != userId) return ResponseEntity.notFound().build()

        val episode = episodeRepository.findById(episodeId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        if (episode.podcastId != podcastId) return ResponseEntity.notFound().build()

        return try {
            val publication = publishingService.publish(episode, podcast, userId, target)
            ResponseEntity.ok(publication.toResponse())
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message))
        } catch (e: IllegalStateException) {
            val message = e.message ?: "Publishing failed"
            if (message.contains("already published")) {
                ResponseEntity.status(409).body(mapOf("error" to message))
            } else {
                ResponseEntity.badRequest().body(mapOf("error" to message))
            }
        } catch (e: Exception) {
            log.error("Publishing failed for episode {} to {}: {}", episodeId, target, e.message, e)
            ResponseEntity.internalServerError().body(mapOf("error" to "Publishing failed: ${e.message}"))
        }
    }

    @GetMapping("/publications")
    fun listPublications(
        @PathVariable userId: String,
        @PathVariable podcastId: String,
        @PathVariable episodeId: Long
    ): ResponseEntity<Any> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val podcast = podcastService.findById(podcastId) ?: return ResponseEntity.notFound().build()
        if (podcast.userId != userId) return ResponseEntity.notFound().build()

        val episode = episodeRepository.findById(episodeId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        if (episode.podcastId != podcastId) return ResponseEntity.notFound().build()

        val publications = publishingService.getPublications(episodeId)
        return ResponseEntity.ok(publications.map { it.toResponse() })
    }

    private fun com.aisummarypodcast.store.EpisodePublication.toResponse() = PublicationResponse(
        id = id!!,
        episodeId = episodeId,
        target = target,
        status = status,
        externalId = externalId,
        externalUrl = externalUrl,
        errorMessage = errorMessage,
        publishedAt = publishedAt,
        createdAt = createdAt
    )
}

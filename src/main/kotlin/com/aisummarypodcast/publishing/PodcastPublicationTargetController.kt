package com.aisummarypodcast.publishing

import com.aisummarypodcast.podcast.PodcastService
import com.aisummarypodcast.user.UserService
import tools.jackson.databind.ObjectMapper
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class PublicationTargetRequest(
    val config: Map<String, Any>? = null,
    val enabled: Boolean = false
)

data class PublicationTargetResponse(
    val target: String,
    val config: Map<String, Any>,
    val enabled: Boolean
)

@RestController
@RequestMapping("/users/{userId}/podcasts/{podcastId}/publication-targets")
class PodcastPublicationTargetController(
    private val userService: UserService,
    private val podcastService: PodcastService,
    private val targetService: PodcastPublicationTargetService,
    private val objectMapper: ObjectMapper
) {

    @GetMapping
    fun list(
        @PathVariable userId: String,
        @PathVariable podcastId: String
    ): ResponseEntity<Any> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val podcast = podcastService.findById(podcastId) ?: return ResponseEntity.notFound().build()
        if (podcast.userId != userId) return ResponseEntity.notFound().build()

        val targets = targetService.list(podcastId).map { it.toResponse() }
        return ResponseEntity.ok(targets)
    }

    @PutMapping("/{target}")
    fun upsert(
        @PathVariable userId: String,
        @PathVariable podcastId: String,
        @PathVariable target: String,
        @RequestBody request: PublicationTargetRequest
    ): ResponseEntity<Any> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val podcast = podcastService.findById(podcastId) ?: return ResponseEntity.notFound().build()
        if (podcast.userId != userId) return ResponseEntity.notFound().build()

        val configJson = objectMapper.writeValueAsString(request.config ?: emptyMap<String, Any>())
        val saved = targetService.upsert(podcastId, target, configJson, request.enabled)
        return ResponseEntity.ok(saved.toResponse())
    }

    @DeleteMapping("/{target}")
    fun delete(
        @PathVariable userId: String,
        @PathVariable podcastId: String,
        @PathVariable target: String
    ): ResponseEntity<Any> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val podcast = podcastService.findById(podcastId) ?: return ResponseEntity.notFound().build()
        if (podcast.userId != userId) return ResponseEntity.notFound().build()

        return if (targetService.delete(podcastId, target)) {
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun com.aisummarypodcast.store.PodcastPublicationTarget.toResponse() = PublicationTargetResponse(
        target = target,
        config = objectMapper.readValue(config, Map::class.java) as Map<String, Any>,
        enabled = enabled
    )
}

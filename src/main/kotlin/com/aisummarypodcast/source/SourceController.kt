package com.aisummarypodcast.source

import com.aisummarypodcast.podcast.PodcastService
import com.aisummarypodcast.user.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI

data class CreateSourceRequest(
    val type: String,
    val url: String,
    val pollIntervalMinutes: Int = 30,
    val enabled: Boolean = true,
    val aggregate: Boolean? = null,
    val maxFailures: Int? = null,
    val maxBackoffHours: Int? = null,
    val pollDelaySeconds: Int? = null,
    val categoryFilter: String? = null
)

data class UpdateSourceRequest(
    val type: String,
    val url: String,
    val pollIntervalMinutes: Int = 30,
    val enabled: Boolean = true,
    val aggregate: Boolean? = null,
    val maxFailures: Int? = null,
    val maxBackoffHours: Int? = null,
    val pollDelaySeconds: Int? = null,
    val categoryFilter: String? = null
)

data class SourceResponse(
    val id: String,
    val podcastId: String,
    val type: String,
    val url: String,
    val pollIntervalMinutes: Int,
    val enabled: Boolean,
    val aggregate: Boolean?,
    val maxFailures: Int?,
    val maxBackoffHours: Int?,
    val pollDelaySeconds: Int?,
    val categoryFilter: String?,
    val createdAt: String,
    val lastPolled: String?,
    val lastSeenId: String?,
    val consecutiveFailures: Int,
    val lastFailureType: String?,
    val disabledReason: String?
)

@RestController
@RequestMapping("/users/{userId}/podcasts/{podcastId}/sources")
class SourceController(
    private val sourceService: SourceService,
    private val podcastService: PodcastService,
    private val userService: UserService
) {

    @PostMapping
    fun create(
        @PathVariable userId: String,
        @PathVariable podcastId: String,
        @RequestBody request: CreateSourceRequest
    ): ResponseEntity<SourceResponse> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val podcast = podcastService.findById(podcastId) ?: return ResponseEntity.notFound().build()
        if (podcast.userId != userId) return ResponseEntity.notFound().build()

        val source = sourceService.create(podcastId, request.type, request.url, request.pollIntervalMinutes, request.enabled, request.aggregate, request.maxFailures, request.maxBackoffHours, request.pollDelaySeconds, request.categoryFilter)
        return ResponseEntity.created(URI.create("/users/$userId/podcasts/$podcastId/sources/${source.id}"))
            .body(source.toResponse())
    }

    @GetMapping
    fun list(@PathVariable userId: String, @PathVariable podcastId: String): ResponseEntity<List<SourceResponse>> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val podcast = podcastService.findById(podcastId) ?: return ResponseEntity.notFound().build()
        if (podcast.userId != userId) return ResponseEntity.notFound().build()

        return ResponseEntity.ok(sourceService.findByPodcastId(podcastId).map { it.toResponse() })
    }

    @PutMapping("/{sourceId}")
    fun update(
        @PathVariable userId: String,
        @PathVariable podcastId: String,
        @PathVariable sourceId: String,
        @RequestBody request: UpdateSourceRequest
    ): ResponseEntity<SourceResponse> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val podcast = podcastService.findById(podcastId) ?: return ResponseEntity.notFound().build()
        if (podcast.userId != userId) return ResponseEntity.notFound().build()

        val source = sourceService.findById(sourceId) ?: return ResponseEntity.notFound().build()
        if (source.podcastId != podcastId) return ResponseEntity.notFound().build()

        val updated = sourceService.update(sourceId, request.type, request.url, request.pollIntervalMinutes, request.enabled, request.aggregate, request.maxFailures, request.maxBackoffHours, request.pollDelaySeconds, request.categoryFilter)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated.toResponse())
    }

    @DeleteMapping("/{sourceId}")
    fun delete(
        @PathVariable userId: String,
        @PathVariable podcastId: String,
        @PathVariable sourceId: String
    ): ResponseEntity<Void> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val podcast = podcastService.findById(podcastId) ?: return ResponseEntity.notFound().build()
        if (podcast.userId != userId) return ResponseEntity.notFound().build()

        val source = sourceService.findById(sourceId) ?: return ResponseEntity.notFound().build()
        if (source.podcastId != podcastId) return ResponseEntity.notFound().build()

        sourceService.delete(sourceId)
        return ResponseEntity.noContent().build()
    }

    private fun com.aisummarypodcast.store.Source.toResponse() = SourceResponse(
        id = id, podcastId = podcastId, type = type, url = url,
        pollIntervalMinutes = pollIntervalMinutes, enabled = enabled, aggregate = aggregate,
        maxFailures = maxFailures, maxBackoffHours = maxBackoffHours, pollDelaySeconds = pollDelaySeconds,
        categoryFilter = categoryFilter, createdAt = createdAt, lastPolled = lastPolled, lastSeenId = lastSeenId,
        consecutiveFailures = consecutiveFailures, lastFailureType = lastFailureType,
        disabledReason = disabledReason
    )
}

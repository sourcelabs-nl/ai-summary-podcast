package com.aisummarypodcast.podcast

import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.EpisodeStatus
import com.aisummarypodcast.user.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users/{userId}/podcasts/{podcastId}/episodes")
class EpisodeController(
    private val podcastService: PodcastService,
    private val userService: UserService,
    private val episodeService: EpisodeService
) {

    @GetMapping
    fun list(
        @PathVariable userId: String,
        @PathVariable podcastId: String,
        @RequestParam(required = false) status: String?
    ): ResponseEntity<Any> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val podcast = podcastService.findById(podcastId) ?: return ResponseEntity.notFound().build()
        if (podcast.userId != userId) return ResponseEntity.notFound().build()

        val episodeStatus = status?.let {
            try { EpisodeStatus.valueOf(it) } catch (_: IllegalArgumentException) {
                return ResponseEntity.badRequest().body(mapOf("error" to "Invalid status: $it. Valid values: ${EpisodeStatus.entries.joinToString()}"))
            }
        }

        val episodes = episodeService.findByPodcastId(podcastId, episodeStatus)

        return ResponseEntity.ok(episodes.map { it.toResponse() })
    }

    @GetMapping("/{episodeId}")
    fun get(
        @PathVariable userId: String,
        @PathVariable podcastId: String,
        @PathVariable episodeId: Long
    ): ResponseEntity<EpisodeResponse> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val podcast = podcastService.findById(podcastId) ?: return ResponseEntity.notFound().build()
        if (podcast.userId != userId) return ResponseEntity.notFound().build()

        val episode = episodeService.findById(episodeId)
            ?: return ResponseEntity.notFound().build()
        if (episode.podcastId != podcastId) return ResponseEntity.notFound().build()

        return ResponseEntity.ok(episode.toResponse())
    }

    @PutMapping("/{episodeId}/script")
    fun editScript(
        @PathVariable userId: String,
        @PathVariable podcastId: String,
        @PathVariable episodeId: Long,
        @RequestBody request: UpdateScriptRequest
    ): ResponseEntity<Any> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val podcast = podcastService.findById(podcastId) ?: return ResponseEntity.notFound().build()
        if (podcast.userId != userId) return ResponseEntity.notFound().build()

        val episode = episodeService.findById(episodeId)
            ?: return ResponseEntity.notFound().build()
        if (episode.podcastId != podcastId) return ResponseEntity.notFound().build()

        if (episode.status != EpisodeStatus.PENDING_REVIEW) {
            return ResponseEntity.status(409).body(mapOf("error" to "Episode is not in PENDING_REVIEW status"))
        }

        val updated = episodeService.updateScript(episode, request.scriptText)
        return ResponseEntity.ok(updated.toResponse())
    }

    @PostMapping("/{episodeId}/approve")
    fun approve(
        @PathVariable userId: String,
        @PathVariable podcastId: String,
        @PathVariable episodeId: Long
    ): ResponseEntity<Any> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val podcast = podcastService.findById(podcastId) ?: return ResponseEntity.notFound().build()
        if (podcast.userId != userId) return ResponseEntity.notFound().build()

        val episode = episodeService.findById(episodeId)
            ?: return ResponseEntity.notFound().build()
        if (episode.podcastId != podcastId) return ResponseEntity.notFound().build()

        if (episode.status != EpisodeStatus.PENDING_REVIEW && episode.status != EpisodeStatus.FAILED) {
            return ResponseEntity.status(409).body(mapOf("error" to "Episode is not in PENDING_REVIEW or FAILED status"))
        }

        episodeService.approveAndGenerateAudio(episode, podcast)

        return ResponseEntity.accepted().body(mapOf("message" to "Episode approved, audio generation started"))
    }

    @PostMapping("/{episodeId}/discard")
    fun discard(
        @PathVariable userId: String,
        @PathVariable podcastId: String,
        @PathVariable episodeId: Long
    ): ResponseEntity<Any> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val podcast = podcastService.findById(podcastId) ?: return ResponseEntity.notFound().build()
        if (podcast.userId != userId) return ResponseEntity.notFound().build()

        val episode = episodeService.findById(episodeId)
            ?: return ResponseEntity.notFound().build()
        if (episode.podcastId != podcastId) return ResponseEntity.notFound().build()

        if (episode.status != EpisodeStatus.PENDING_REVIEW && episode.status != EpisodeStatus.GENERATED && episode.status != EpisodeStatus.FAILED) {
            val message = if (episode.status == EpisodeStatus.GENERATING_AUDIO) "Audio generation is in progress" else "Episode is not in a discardable status"
            return ResponseEntity.status(409).body(mapOf("error" to message))
        }

        if (episode.status == EpisodeStatus.FAILED) {
            episodeService.discardOnly(episode, podcastId)
        } else {
            episodeService.discardAndResetArticles(episode, podcastId)
        }
        return ResponseEntity.ok(mapOf("message" to "Episode discarded"))
    }

    @PostMapping("/{episodeId}/regenerate-recap")
    fun regenerateRecap(
        @PathVariable userId: String,
        @PathVariable podcastId: String,
        @PathVariable episodeId: Long
    ): ResponseEntity<Any> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val podcast = podcastService.findById(podcastId) ?: return ResponseEntity.notFound().build()
        if (podcast.userId != userId) return ResponseEntity.notFound().build()

        val episode = episodeService.findById(episodeId)
            ?: return ResponseEntity.notFound().build()
        if (episode.podcastId != podcastId) return ResponseEntity.notFound().build()

        return try {
            val updated = episodeService.regenerateRecap(episode, podcast)
            ResponseEntity.ok(updated.toResponse())
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(mapOf("error" to "Failed to regenerate recap: ${e.message}"))
        }
    }

    @PostMapping("/{episodeId}/retry")
    fun retry(
        @PathVariable userId: String,
        @PathVariable podcastId: String,
        @PathVariable episodeId: Long
    ): ResponseEntity<Any> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val podcast = podcastService.findById(podcastId) ?: return ResponseEntity.notFound().build()
        if (podcast.userId != userId) return ResponseEntity.notFound().build()

        val episode = episodeService.findById(episodeId)
            ?: return ResponseEntity.notFound().build()
        if (episode.podcastId != podcastId) return ResponseEntity.notFound().build()

        if (episode.status != EpisodeStatus.FAILED) {
            return ResponseEntity.status(409).body(mapOf("error" to "Episode is not in FAILED status"))
        }

        val resumePoint = podcastService.retryEpisode(episode, podcast)
        return ResponseEntity.accepted().body(mapOf(
            "message" to "Retrying episode from ${resumePoint.name.lowercase().replace('_', ' ')}",
            "resumePoint" to resumePoint.name
        ))
    }

    @GetMapping("/{episodeId}/articles")
    fun articles(
        @PathVariable userId: String,
        @PathVariable podcastId: String,
        @PathVariable episodeId: Long
    ): ResponseEntity<List<EpisodeArticleResponse>> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val podcast = podcastService.findById(podcastId) ?: return ResponseEntity.notFound().build()
        if (podcast.userId != userId) return ResponseEntity.notFound().build()

        val episode = episodeService.findById(episodeId)
            ?: return ResponseEntity.notFound().build()
        if (episode.podcastId != podcastId) return ResponseEntity.notFound().build()

        return ResponseEntity.ok(episodeService.findArticlesForEpisode(episodeId))
    }

}

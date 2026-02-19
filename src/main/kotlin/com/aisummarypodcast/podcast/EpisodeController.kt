package com.aisummarypodcast.podcast

import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.EpisodeArticleRepository
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.user.UserService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class EpisodeResponse(
    val id: Long,
    val podcastId: String,
    val generatedAt: String,
    val scriptText: String,
    val status: String,
    val audioFilePath: String?,
    val durationSeconds: Int?,
    val filterModel: String?,
    val composeModel: String?,
    val llmInputTokens: Int?,
    val llmOutputTokens: Int?,
    val llmCostCents: Int?,
    val ttsCharacters: Int?,
    val ttsCostCents: Int?,
    val ttsModel: String?
)

data class UpdateScriptRequest(
    val scriptText: String
)

@RestController
@RequestMapping("/users/{userId}/podcasts/{podcastId}/episodes")
class EpisodeController(
    private val episodeRepository: EpisodeRepository,
    private val podcastService: PodcastService,
    private val userService: UserService,
    private val episodeService: EpisodeService,
    private val episodeArticleRepository: EpisodeArticleRepository,
    private val articleRepository: ArticleRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun list(
        @PathVariable userId: String,
        @PathVariable podcastId: String,
        @RequestParam(required = false) status: String?
    ): ResponseEntity<List<EpisodeResponse>> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val podcast = podcastService.findById(podcastId) ?: return ResponseEntity.notFound().build()
        if (podcast.userId != userId) return ResponseEntity.notFound().build()

        val episodes = if (status != null) {
            episodeRepository.findByPodcastIdAndStatus(podcastId, status)
        } else {
            episodeRepository.findByPodcastId(podcastId)
        }

        return ResponseEntity.ok(episodes.sortedByDescending { it.generatedAt }.map { it.toResponse() })
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

        val episode = episodeRepository.findById(episodeId).orElse(null)
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

        val episode = episodeRepository.findById(episodeId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        if (episode.podcastId != podcastId) return ResponseEntity.notFound().build()

        if (episode.status != "PENDING_REVIEW") {
            return ResponseEntity.status(409).body(mapOf("error" to "Episode is not in PENDING_REVIEW status"))
        }

        val updated = episodeRepository.save(episode.copy(scriptText = request.scriptText))
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

        val episode = episodeRepository.findById(episodeId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        if (episode.podcastId != podcastId) return ResponseEntity.notFound().build()

        if (episode.status != "PENDING_REVIEW" && episode.status != "FAILED") {
            return ResponseEntity.status(409).body(mapOf("error" to "Episode is not in PENDING_REVIEW or FAILED status"))
        }

        episodeRepository.save(episode.copy(status = "APPROVED"))
        log.info("Episode {} approved, triggering async TTS generation", episodeId)
        episodeService.generateAudioAsync(episodeId, podcastId)

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

        val episode = episodeRepository.findById(episodeId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        if (episode.podcastId != podcastId) return ResponseEntity.notFound().build()

        if (episode.status != "PENDING_REVIEW") {
            return ResponseEntity.status(409).body(mapOf("error" to "Episode is not in PENDING_REVIEW status"))
        }

        episodeRepository.save(episode.copy(status = "DISCARDED"))

        val linkedArticles = episodeArticleRepository.findByEpisodeId(episodeId)
        if (linkedArticles.isEmpty()) {
            log.warn("Episode {} has no episode-article links; cannot reset articles for reprocessing", episodeId)
        } else {
            for (link in linkedArticles) {
                articleRepository.findById(link.articleId).ifPresent { article ->
                    articleRepository.save(article.copy(isProcessed = false))
                }
            }
            log.info("Episode {} discarded, reset {} linked articles for reprocessing", episodeId, linkedArticles.size)
        }
        return ResponseEntity.ok(mapOf("message" to "Episode discarded"))
    }

    private fun com.aisummarypodcast.store.Episode.toResponse() = EpisodeResponse(
        id = id!!,
        podcastId = podcastId,
        generatedAt = generatedAt,
        scriptText = scriptText,
        status = status,
        audioFilePath = audioFilePath,
        durationSeconds = durationSeconds,
        filterModel = filterModel,
        composeModel = composeModel,
        llmInputTokens = llmInputTokens,
        llmOutputTokens = llmOutputTokens,
        llmCostCents = llmCostCents,
        ttsCharacters = ttsCharacters,
        ttsCostCents = ttsCostCents,
        ttsModel = ttsModel
    )
}

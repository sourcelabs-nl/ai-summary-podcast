package com.aisummarypodcast.podcast

import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.store.EpisodeStatus
import com.aisummarypodcast.user.UserService
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.simple.JdbcClient
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
    val ttsModel: String?,
    val recap: String?,
    val showNotes: String?,
    val errorMessage: String?,
    val pipelineStage: String?
)

data class UpdateScriptRequest(
    val scriptText: String
)

data class ArticleSourceResponse(
    val id: String,
    val type: String,
    val url: String,
    val label: String?
)

data class EpisodeArticleResponse(
    val id: Long,
    val title: String,
    val url: String,
    val author: String?,
    val publishedAt: String?,
    val relevanceScore: Int?,
    val summary: String?,
    val body: String?,
    val source: ArticleSourceResponse
)

@RestController
@RequestMapping("/users/{userId}/podcasts/{podcastId}/episodes")
class EpisodeController(
    private val episodeRepository: EpisodeRepository,
    private val podcastService: PodcastService,
    private val userService: UserService,
    private val episodeService: EpisodeService,
    private val jdbcClient: JdbcClient
) {

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

        return ResponseEntity.ok(episodes.sortedWith(compareByDescending<Episode> { it.generatedAt }.thenByDescending { it.id }).map { it.toResponse() })
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

        val episode = episodeRepository.findById(episodeId).orElse(null)
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

        val episode = episodeRepository.findById(episodeId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        if (episode.podcastId != podcastId) return ResponseEntity.notFound().build()

        if (episode.status != EpisodeStatus.PENDING_REVIEW && episode.status != EpisodeStatus.GENERATED) {
            return ResponseEntity.status(409).body(mapOf("error" to "Episode is not in PENDING_REVIEW or GENERATED status"))
        }

        episodeService.discardAndResetArticles(episode, podcastId)
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

        val episode = episodeRepository.findById(episodeId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        if (episode.podcastId != podcastId) return ResponseEntity.notFound().build()

        return try {
            val updated = episodeService.regenerateRecap(episode, podcast)
            ResponseEntity.ok(updated.toResponse())
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(mapOf("error" to "Failed to regenerate recap: ${e.message}"))
        }
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

        val episode = episodeRepository.findById(episodeId).orElse(null)
            ?: return ResponseEntity.notFound().build()
        if (episode.podcastId != podcastId) return ResponseEntity.notFound().build()

        val articles = jdbcClient.sql(
            """
            SELECT a.id, a.title, a.url, a.author, a.published_at, a.relevance_score, a.summary, a.body,
                   s.id AS source_id, s.type AS source_type, s.url AS source_url, s.label AS source_label
            FROM episode_articles ea
            JOIN articles a ON ea.article_id = a.id
            JOIN sources s ON a.source_id = s.id
            WHERE ea.episode_id = :episodeId
            ORDER BY a.relevance_score DESC NULLS LAST
            """.trimIndent()
        )
            .param("episodeId", episodeId)
            .query { rs, _ ->
                EpisodeArticleResponse(
                    id = rs.getLong("id"),
                    title = rs.getString("title"),
                    url = rs.getString("url"),
                    author = rs.getString("author"),
                    publishedAt = rs.getString("published_at"),
                    relevanceScore = rs.getObject("relevance_score") as? Int,
                    summary = rs.getString("summary"),
                    body = rs.getString("body"),
                    source = ArticleSourceResponse(
                        id = rs.getString("source_id"),
                        type = rs.getString("source_type"),
                        url = rs.getString("source_url"),
                        label = rs.getString("source_label")
                    )
                )
            }
            .list()

        return ResponseEntity.ok(articles)
    }

    private fun Episode.toResponse() = EpisodeResponse(
        id = id!!,
        podcastId = podcastId,
        generatedAt = generatedAt,
        scriptText = scriptText,
        status = status.name,
        audioFilePath = audioFilePath,
        durationSeconds = durationSeconds,
        filterModel = filterModel,
        composeModel = composeModel,
        llmInputTokens = llmInputTokens,
        llmOutputTokens = llmOutputTokens,
        llmCostCents = llmCostCents,
        ttsCharacters = ttsCharacters,
        ttsCostCents = ttsCostCents,
        ttsModel = ttsModel,
        recap = recap,
        showNotes = showNotes,
        errorMessage = errorMessage,
        pipelineStage = pipelineStage
    )
}

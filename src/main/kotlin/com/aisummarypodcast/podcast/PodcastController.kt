package com.aisummarypodcast.podcast

import com.aisummarypodcast.llm.LlmPipeline
import com.aisummarypodcast.tts.TtsPipeline
import com.aisummarypodcast.user.UserService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.fasterxml.jackson.annotation.JsonProperty
import java.net.URI
import java.time.Instant

data class CreatePodcastRequest(
    val name: String,
    val topic: String,
    val language: String? = null,
    val llmModels: Map<String, String>? = null,
    val ttsVoice: String? = null,
    @JsonProperty("ttsSpeed") val ttsSpeed: Double? = null,
    val style: String? = null,
    @JsonProperty("targetWords") val targetWords: Int? = null,
    val cron: String? = null,
    val customInstructions: String? = null,
    @JsonProperty("relevanceThreshold") val relevanceThreshold: Int? = null,
    @JsonProperty("requireReview") val requireReview: Boolean? = null,
    @JsonProperty("maxLlmCostCents") val maxLlmCostCents: Int? = null,
    @JsonProperty("maxArticleAgeDays") val maxArticleAgeDays: Int? = null
)

data class UpdatePodcastRequest(
    val name: String,
    val topic: String,
    val language: String? = null,
    val llmModels: Map<String, String>? = null,
    val ttsVoice: String? = null,
    @JsonProperty("ttsSpeed") val ttsSpeed: Double? = null,
    val style: String? = null,
    @JsonProperty("targetWords") val targetWords: Int? = null,
    val cron: String? = null,
    val customInstructions: String? = null,
    @JsonProperty("relevanceThreshold") val relevanceThreshold: Int? = null,
    @JsonProperty("requireReview") val requireReview: Boolean? = null,
    @JsonProperty("maxLlmCostCents") val maxLlmCostCents: Int? = null,
    @JsonProperty("maxArticleAgeDays") val maxArticleAgeDays: Int? = null
)

data class PodcastResponse(
    val id: String,
    val userId: String,
    val name: String,
    val topic: String,
    val language: String,
    val llmModels: Map<String, String>?,
    val ttsVoice: String,
    val ttsSpeed: Double,
    val style: String,
    val targetWords: Int?,
    val cron: String,
    val customInstructions: String?,
    val relevanceThreshold: Int,
    val requireReview: Boolean,
    val maxLlmCostCents: Int?,
    val maxArticleAgeDays: Int?,
    val lastGeneratedAt: String?
)

@RestController
@RequestMapping("/users/{userId}/podcasts")
class PodcastController(
    private val podcastService: PodcastService,
    private val userService: UserService,
    private val llmPipeline: LlmPipeline,
    private val ttsPipeline: TtsPipeline,
    private val episodeRepository: com.aisummarypodcast.store.EpisodeRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping
    fun create(@PathVariable userId: String, @RequestBody request: CreatePodcastRequest): ResponseEntity<Any> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val language = request.language ?: "en"
        if (!SupportedLanguage.isSupported(language)) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Unsupported language: $language"))
        }
        val podcast = podcastService.create(
            userId = userId,
            name = request.name,
            topic = request.topic,
            podcast = com.aisummarypodcast.store.Podcast(
                id = "",
                userId = userId,
                name = request.name,
                topic = request.topic,
                language = language,
                llmModels = request.llmModels,
                ttsVoice = request.ttsVoice ?: "nova",
                ttsSpeed = request.ttsSpeed ?: 1.0,
                style = request.style ?: "news-briefing",
                targetWords = request.targetWords,
                cron = request.cron ?: "0 0 6 * * *",
                customInstructions = request.customInstructions,
                relevanceThreshold = request.relevanceThreshold ?: 5,
                requireReview = request.requireReview ?: false,
                maxLlmCostCents = request.maxLlmCostCents,
                maxArticleAgeDays = request.maxArticleAgeDays
            )
        )
        return ResponseEntity.created(URI.create("/users/$userId/podcasts/${podcast.id}"))
            .body(podcast.toResponse())
    }

    @GetMapping
    fun list(@PathVariable userId: String): ResponseEntity<List<PodcastResponse>> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(podcastService.findByUserId(userId).map { it.toResponse() })
    }

    @GetMapping("/{podcastId}")
    fun get(@PathVariable userId: String, @PathVariable podcastId: String): ResponseEntity<PodcastResponse> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val podcast = podcastService.findById(podcastId) ?: return ResponseEntity.notFound().build()
        if (podcast.userId != userId) return ResponseEntity.notFound().build()
        return ResponseEntity.ok(podcast.toResponse())
    }

    @PutMapping("/{podcastId}")
    fun update(
        @PathVariable userId: String,
        @PathVariable podcastId: String,
        @RequestBody request: UpdatePodcastRequest
    ): ResponseEntity<Any> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val existing = podcastService.findById(podcastId) ?: return ResponseEntity.notFound().build()
        if (existing.userId != userId) return ResponseEntity.notFound().build()
        if (request.language != null && !SupportedLanguage.isSupported(request.language)) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Unsupported language: ${request.language}"))
        }
        val updated = podcastService.update(
            podcastId,
            existing.copy(
                name = request.name,
                topic = request.topic,
                language = request.language ?: existing.language,
                llmModels = request.llmModels ?: existing.llmModels,
                ttsVoice = request.ttsVoice ?: existing.ttsVoice,
                ttsSpeed = request.ttsSpeed ?: existing.ttsSpeed,
                style = request.style ?: existing.style,
                targetWords = request.targetWords ?: existing.targetWords,
                cron = request.cron ?: existing.cron,
                customInstructions = request.customInstructions ?: existing.customInstructions,
                relevanceThreshold = request.relevanceThreshold ?: existing.relevanceThreshold,
                requireReview = request.requireReview ?: existing.requireReview,
                maxLlmCostCents = request.maxLlmCostCents ?: existing.maxLlmCostCents,
                maxArticleAgeDays = request.maxArticleAgeDays ?: existing.maxArticleAgeDays
            )
        ) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(updated.toResponse())
    }

    @DeleteMapping("/{podcastId}")
    fun delete(@PathVariable userId: String, @PathVariable podcastId: String): ResponseEntity<Void> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val podcast = podcastService.findById(podcastId) ?: return ResponseEntity.notFound().build()
        if (podcast.userId != userId) return ResponseEntity.notFound().build()
        podcastService.delete(podcastId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{podcastId}/generate")
    fun generate(@PathVariable userId: String, @PathVariable podcastId: String): ResponseEntity<Any> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val podcast = podcastService.findById(podcastId) ?: return ResponseEntity.notFound().build()
        if (podcast.userId != userId) return ResponseEntity.notFound().build()

        if (podcast.requireReview) {
            val pending = episodeRepository.findByPodcastIdAndStatusIn(
                podcastId, listOf("PENDING_REVIEW", "APPROVED")
            )
            if (pending.isNotEmpty()) {
                return ResponseEntity.status(409).body(mapOf("error" to "A pending or approved episode already exists. Approve or discard it first."))
            }
        }

        log.info("Manual briefing generation triggered for podcast {}", podcastId)
        val result = llmPipeline.run(podcast)
            ?: return ResponseEntity.ok("No relevant articles to process")

        if (podcast.requireReview) {
            val episode = episodeRepository.save(
                com.aisummarypodcast.store.Episode(
                    podcastId = podcastId,
                    generatedAt = Instant.now().toString(),
                    scriptText = result.script,
                    status = "PENDING_REVIEW",
                    filterModel = result.filterModel,
                    composeModel = result.composeModel,
                    llmInputTokens = result.llmInputTokens,
                    llmOutputTokens = result.llmOutputTokens,
                    llmCostCents = result.llmCostCents
                )
            )
            podcastService.update(podcastId, podcast.copy(lastGeneratedAt = Instant.now().toString()))
            return ResponseEntity.ok(mapOf("message" to "Script ready for review", "episodeId" to episode.id))
        }

        val episode = ttsPipeline.generate(result.script, podcast)
        val episodeWithModels = episode.copy(
            filterModel = result.filterModel,
            composeModel = result.composeModel,
            llmInputTokens = result.llmInputTokens,
            llmOutputTokens = result.llmOutputTokens,
            llmCostCents = result.llmCostCents
        )
        episodeRepository.save(episodeWithModels)
        podcastService.update(podcastId, podcast.copy(lastGeneratedAt = Instant.now().toString()))
        return ResponseEntity.ok(mapOf("message" to "Episode generated: ${episodeWithModels.id} (${episodeWithModels.durationSeconds}s)"))
    }

    private fun com.aisummarypodcast.store.Podcast.toResponse() = PodcastResponse(
        id = id, userId = userId, name = name, topic = topic,
        language = language, llmModels = llmModels, ttsVoice = ttsVoice, ttsSpeed = ttsSpeed,
        style = style, targetWords = targetWords, cron = cron,
        customInstructions = customInstructions, relevanceThreshold = relevanceThreshold,
        requireReview = requireReview, maxLlmCostCents = maxLlmCostCents,
        maxArticleAgeDays = maxArticleAgeDays, lastGeneratedAt = lastGeneratedAt
    )
}

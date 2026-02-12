package com.aisummarypodcast.podcast

import com.aisummarypodcast.llm.LlmPipeline
import com.aisummarypodcast.tts.TtsPipeline
import com.aisummarypodcast.user.UserService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI
import java.time.Instant

data class CreatePodcastRequest(
    val name: String,
    val topic: String,
    val language: String? = null,
    val llmModel: String? = null,
    val ttsVoice: String? = null,
    val ttsSpeed: Double? = null,
    val style: String? = null,
    val targetWords: Int? = null,
    val cron: String? = null,
    val customInstructions: String? = null
)

data class UpdatePodcastRequest(
    val name: String,
    val topic: String,
    val language: String? = null,
    val llmModel: String? = null,
    val ttsVoice: String? = null,
    val ttsSpeed: Double? = null,
    val style: String? = null,
    val targetWords: Int? = null,
    val cron: String? = null,
    val customInstructions: String? = null
)

data class PodcastResponse(
    val id: String,
    val userId: String,
    val name: String,
    val topic: String,
    val language: String,
    val llmModel: String?,
    val ttsVoice: String,
    val ttsSpeed: Double,
    val style: String,
    val targetWords: Int?,
    val cron: String,
    val customInstructions: String?,
    val lastGeneratedAt: String?
)

@RestController
@RequestMapping("/users/{userId}/podcasts")
class PodcastController(
    private val podcastService: PodcastService,
    private val userService: UserService,
    private val llmPipeline: LlmPipeline,
    private val ttsPipeline: TtsPipeline
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
                llmModel = request.llmModel,
                ttsVoice = request.ttsVoice ?: "nova",
                ttsSpeed = request.ttsSpeed ?: 1.0,
                style = request.style ?: "news-briefing",
                targetWords = request.targetWords,
                cron = request.cron ?: "0 0 6 * * *",
                customInstructions = request.customInstructions
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
                llmModel = request.llmModel ?: existing.llmModel,
                ttsVoice = request.ttsVoice ?: existing.ttsVoice,
                ttsSpeed = request.ttsSpeed ?: existing.ttsSpeed,
                style = request.style ?: existing.style,
                targetWords = request.targetWords ?: existing.targetWords,
                cron = request.cron ?: existing.cron,
                customInstructions = request.customInstructions ?: existing.customInstructions
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
    fun generate(@PathVariable userId: String, @PathVariable podcastId: String): ResponseEntity<String> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val podcast = podcastService.findById(podcastId) ?: return ResponseEntity.notFound().build()
        if (podcast.userId != userId) return ResponseEntity.notFound().build()

        log.info("Manual briefing generation triggered for podcast {}", podcastId)
        val script = llmPipeline.run(podcast)
            ?: return ResponseEntity.ok("No relevant articles to process")

        val episode = ttsPipeline.generate(script, podcast)
        podcastService.update(podcastId, podcast.copy(lastGeneratedAt = Instant.now().toString()))
        return ResponseEntity.ok("Episode generated: ${episode.id} (${episode.durationSeconds}s)")
    }

    private fun com.aisummarypodcast.store.Podcast.toResponse() = PodcastResponse(
        id = id, userId = userId, name = name, topic = topic,
        language = language, llmModel = llmModel, ttsVoice = ttsVoice, ttsSpeed = ttsSpeed,
        style = style, targetWords = targetWords, cron = cron,
        customInstructions = customInstructions, lastGeneratedAt = lastGeneratedAt
    )
}

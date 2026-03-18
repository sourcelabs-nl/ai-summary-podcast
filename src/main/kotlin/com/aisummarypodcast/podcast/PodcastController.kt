package com.aisummarypodcast.podcast

import com.aisummarypodcast.source.SourceAggregator
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PodcastStyle
import com.aisummarypodcast.store.TtsProviderType
import com.aisummarypodcast.user.UserService
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URI

data class CreatePodcastRequest(
    val name: String,
    val topic: String,
    val language: String? = null,
    val llmModels: Map<String, String>? = null,
    val ttsProvider: String? = null,
    val ttsVoices: Map<String, String>? = null,
    val ttsSettings: Map<String, String>? = null,
    val style: String? = null,
    @JsonProperty("targetWords") val targetWords: Int? = null,
    val cron: String? = null,
    val customInstructions: String? = null,
    @JsonProperty("relevanceThreshold") val relevanceThreshold: Int? = null,
    @JsonProperty("requireReview") val requireReview: Boolean? = null,
    @JsonProperty("maxLlmCostCents") val maxLlmCostCents: Int? = null,
    @JsonProperty("maxArticleAgeDays") val maxArticleAgeDays: Int? = null,
    val speakerNames: Map<String, String>? = null,
    @JsonProperty("fullBodyThreshold") val fullBodyThreshold: Int? = null,
    val sponsor: Map<String, String>? = null,
    val pronunciations: Map<String, String>? = null
)

data class UpdatePodcastRequest(
    val name: String,
    val topic: String,
    val language: String? = null,
    val llmModels: Map<String, String>? = null,
    val ttsProvider: String? = null,
    val ttsVoices: Map<String, String>? = null,
    val ttsSettings: Map<String, String>? = null,
    val style: String? = null,
    @JsonProperty("targetWords") val targetWords: Int? = null,
    val cron: String? = null,
    val customInstructions: String? = null,
    @JsonProperty("relevanceThreshold") val relevanceThreshold: Int? = null,
    @JsonProperty("requireReview") val requireReview: Boolean? = null,
    @JsonProperty("maxLlmCostCents") val maxLlmCostCents: Int? = null,
    @JsonProperty("maxArticleAgeDays") val maxArticleAgeDays: Int? = null,
    val speakerNames: Map<String, String>? = null,
    @JsonProperty("fullBodyThreshold") val fullBodyThreshold: Int? = null,
    val sponsor: Map<String, String>? = null,
    val pronunciations: Map<String, String>? = null
)

data class PodcastResponse(
    val id: String,
    val userId: String,
    val name: String,
    val topic: String,
    val language: String,
    val llmModels: Map<String, String>?,
    val ttsProvider: String,
    val ttsVoices: Map<String, String>?,
    val ttsSettings: Map<String, String>?,
    val style: String,
    val targetWords: Int?,
    val cron: String,
    val customInstructions: String?,
    val relevanceThreshold: Int,
    val requireReview: Boolean,
    val maxLlmCostCents: Int?,
    val maxArticleAgeDays: Int?,
    val speakerNames: Map<String, String>?,
    val fullBodyThreshold: Int?,
    val sponsor: Map<String, String>?,
    val pronunciations: Map<String, String>?,
    val lastGeneratedAt: String?
)

/**
 * Nullable update helper: absent (null) keeps existing value, empty clears to null, non-empty updates.
 * Allows clearing nullable fields via the API by sending "" or {}.
 */
private fun String?.orKeep(existing: String?): String? = when {
    this == null -> existing
    this.isEmpty() -> null
    else -> this
}

private fun Map<String, String>?.orKeep(existing: Map<String, String>?): Map<String, String>? = when {
    this == null -> existing
    this.isEmpty() -> null
    else -> this
}

@RestController
@RequestMapping("/users/{userId}/podcasts")
class PodcastController(
    private val podcastService: PodcastService,
    private val userService: UserService,
    private val episodeService: EpisodeService,
    private val objectMapper: ObjectMapper,
    private val sourceAggregator: SourceAggregator
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val previewScope = CoroutineScope(Dispatchers.IO)
    private val dialogueProviders = setOf(TtsProviderType.ELEVENLABS, TtsProviderType.INWORLD)

    private fun validateTtsConfig(ttsProvider: TtsProviderType, style: PodcastStyle, ttsVoices: Map<String, String>?): String? {
        if (style == PodcastStyle.DIALOGUE && ttsProvider !in dialogueProviders) {
            return "Dialogue style requires ElevenLabs or Inworld as TTS provider"
        }
        if (style == PodcastStyle.DIALOGUE && (ttsVoices == null || ttsVoices.size < 2)) {
            return "Dialogue style requires at least two voice roles in ttsVoices (e.g., host and cohost)"
        }
        if (style == PodcastStyle.INTERVIEW && ttsProvider !in dialogueProviders) {
            return "Interview style requires ElevenLabs or Inworld as TTS provider"
        }
        if (style == PodcastStyle.INTERVIEW && (ttsVoices == null || ttsVoices.size < 2)) {
            return "Interview style requires at least two voice roles in ttsVoices (interviewer and expert)"
        }
        if (style == PodcastStyle.INTERVIEW && ttsVoices != null && ttsVoices.keys != setOf("interviewer", "expert")) {
            return "Interview style requires exactly 'interviewer' and 'expert' voice roles"
        }
        return null
    }

    @PostMapping
    fun create(@PathVariable userId: String, @RequestBody request: CreatePodcastRequest): ResponseEntity<Any> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val language = request.language ?: "en"
        if (!SupportedLanguage.isSupported(language)) {
            return ResponseEntity.badRequest().body(mapOf("error" to "Unsupported language: $language"))
        }
        val ttsProvider = request.ttsProvider?.let {
            TtsProviderType.fromValue(it)
                ?: return ResponseEntity.badRequest().body(mapOf("error" to "Unsupported TTS provider: $it. Supported: ${TtsProviderType.entries.joinToString { e -> e.value }}"))
        } ?: TtsProviderType.OPENAI
        val style = request.style?.let {
            PodcastStyle.fromValue(it)
                ?: return ResponseEntity.badRequest().body(mapOf("error" to "Unsupported style: $it. Supported: ${PodcastStyle.entries.joinToString { e -> e.value }}"))
        } ?: PodcastStyle.NEWS_BRIEFING
        validateTtsConfig(ttsProvider, style, request.ttsVoices)?.let {
            return ResponseEntity.badRequest().body(mapOf("error" to it))
        }
        if (request.fullBodyThreshold != null && request.fullBodyThreshold < 1) {
            return ResponseEntity.badRequest().body(mapOf("error" to "fullBodyThreshold must be at least 1"))
        }
        val podcast = podcastService.create(
            userId = userId,
            name = request.name,
            topic = request.topic,
            podcast = Podcast(
                id = "",
                userId = userId,
                name = request.name,
                topic = request.topic,
                language = language,
                llmModels = request.llmModels,
                ttsProvider = ttsProvider,
                ttsVoices = request.ttsVoices,
                ttsSettings = request.ttsSettings,
                style = style,
                targetWords = request.targetWords,
                cron = request.cron ?: "0 0 6 * * *",
                customInstructions = request.customInstructions,
                relevanceThreshold = request.relevanceThreshold ?: 5,
                requireReview = request.requireReview ?: false,
                maxLlmCostCents = request.maxLlmCostCents,
                maxArticleAgeDays = request.maxArticleAgeDays,
                speakerNames = request.speakerNames,
                fullBodyThreshold = request.fullBodyThreshold,
                sponsor = request.sponsor,
                pronunciations = request.pronunciations
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
        val effectiveTtsProvider = request.ttsProvider?.let {
            TtsProviderType.fromValue(it)
                ?: return ResponseEntity.badRequest().body(mapOf("error" to "Unsupported TTS provider: $it. Supported: ${TtsProviderType.entries.joinToString { e -> e.value }}"))
        } ?: existing.ttsProvider
        val effectiveStyle = request.style?.let {
            PodcastStyle.fromValue(it)
                ?: return ResponseEntity.badRequest().body(mapOf("error" to "Unsupported style: $it. Supported: ${PodcastStyle.entries.joinToString { e -> e.value }}"))
        } ?: existing.style
        val effectiveVoices = request.ttsVoices.orKeep(existing.ttsVoices)
        validateTtsConfig(effectiveTtsProvider, effectiveStyle, effectiveVoices)?.let {
            return ResponseEntity.badRequest().body(mapOf("error" to it))
        }
        if (request.fullBodyThreshold != null && request.fullBodyThreshold < 1) {
            return ResponseEntity.badRequest().body(mapOf("error" to "fullBodyThreshold must be at least 1"))
        }
        val updated = podcastService.update(
            podcastId,
            existing.copy(
                name = request.name,
                topic = request.topic,
                language = request.language ?: existing.language,
                llmModels = request.llmModels.orKeep(existing.llmModels),
                ttsProvider = effectiveTtsProvider,
                ttsVoices = request.ttsVoices.orKeep(existing.ttsVoices),
                ttsSettings = request.ttsSettings.orKeep(existing.ttsSettings),
                style = effectiveStyle,
                targetWords = request.targetWords,
                cron = request.cron ?: existing.cron,
                customInstructions = request.customInstructions.orKeep(existing.customInstructions),
                relevanceThreshold = request.relevanceThreshold ?: existing.relevanceThreshold,
                requireReview = request.requireReview ?: existing.requireReview,
                maxLlmCostCents = request.maxLlmCostCents,
                maxArticleAgeDays = request.maxArticleAgeDays,
                speakerNames = request.speakerNames.orKeep(existing.speakerNames),
                fullBodyThreshold = request.fullBodyThreshold,
                sponsor = request.sponsor.orKeep(existing.sponsor),
                pronunciations = request.pronunciations.orKeep(existing.pronunciations)
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

        log.info("Manual briefing generation triggered for podcast {}", podcastId)

        if (podcast.requireReview && episodeService.hasPendingOrApprovedEpisode(podcastId)) {
            return ResponseEntity.status(409).body(mapOf("error" to "A pending or approved episode already exists — approve or discard it first"))
        }

        val episode = podcastService.generateBriefing(podcast)
            ?: return ResponseEntity.ok(mapOf("message" to "No relevant articles to process"))

        return if (podcast.requireReview) {
            ResponseEntity.ok(mapOf("message" to "Script ready for review", "episodeId" to episode.id))
        } else {
            ResponseEntity.ok(mapOf("message" to "Episode generated: ${episode.id} (${episode.durationSeconds}s)", "episodeId" to episode.id))
        }
    }

    @PostMapping("/{podcastId}/episodes/{episodeId}/regenerate")
    fun regenerate(
        @PathVariable userId: String,
        @PathVariable podcastId: String,
        @PathVariable episodeId: Long
    ): ResponseEntity<Any> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val podcast = podcastService.findById(podcastId) ?: return ResponseEntity.notFound().build()
        if (podcast.userId != userId) return ResponseEntity.notFound().build()
        val episode = episodeService.findById(episodeId) ?: return ResponseEntity.notFound().build()
        if (episode.podcastId != podcastId) return ResponseEntity.notFound().build()

        log.info("Regenerate triggered for episode {} of podcast {}", episodeId, podcastId)
        val newEpisode = podcastService.regenerateEpisode(episode, podcast)
        return ResponseEntity.ok(mapOf("message" to "Episode regenerated", "episodeId" to newEpisode.id))
    }

    @GetMapping("/{podcastId}/upcoming-articles")
    fun upcomingArticles(@PathVariable userId: String, @PathVariable podcastId: String): ResponseEntity<Any> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val podcast = podcastService.findById(podcastId) ?: return ResponseEntity.notFound().build()
        if (podcast.userId != userId) return ResponseEntity.notFound().build()

        val content = podcastService.getUpcomingContent(podcast)
        val sourceMap = content.sources.associateBy { it.id }

        val articleResponses = content.articles.map { article ->
            val source = sourceMap[article.sourceId]
            EpisodeArticleResponse(
                id = article.id!!,
                title = article.title,
                url = article.url,
                author = article.author,
                publishedAt = article.publishedAt,
                relevanceScore = article.relevanceScore,
                summary = article.summary,
                body = article.body,
                source = ArticleSourceResponse(
                    id = source?.id ?: article.sourceId,
                    type = source?.type?.name ?: "UNKNOWN",
                    url = source?.url ?: "",
                    label = source?.label
                )
            )
        }

        val postResponses = content.unlinkedPosts.map { post ->
            val source = sourceMap[post.sourceId]
            EpisodeArticleResponse(
                id = post.id!!,
                title = post.title,
                url = post.url,
                author = post.author,
                publishedAt = post.publishedAt,
                relevanceScore = null,
                summary = null,
                body = post.body,
                source = ArticleSourceResponse(
                    id = source?.id ?: post.sourceId,
                    type = source?.type?.name ?: "UNKNOWN",
                    url = source?.url ?: "",
                    label = source?.label
                )
            )
        }

        val allArticles = (articleResponses + postResponses).sortedByDescending { it.relevanceScore }

        // Pre-calculate effective article count after aggregation:
        // unlinked posts from aggregatable sources count as 1 article per source
        val unlinkedPostArticleCount = content.unlinkedPosts
            .groupBy { it.sourceId }
            .entries
            .sumOf { (sourceId, posts) ->
                val source = sourceMap[sourceId]
                if (source != null && sourceAggregator.shouldAggregate(source) && posts.size > 1) 1L else posts.size.toLong()
            }
        val effectiveArticleCount = content.articles.size + unlinkedPostArticleCount

        val response = mapOf(
            "articles" to allArticles,
            "articleCount" to effectiveArticleCount,
            "postCount" to content.totalPostCount
        )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/{podcastId}/preview", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun preview(@PathVariable userId: String, @PathVariable podcastId: String): Any {
        userService.findById(userId) ?: return ResponseEntity.notFound().build<Any>()
        val podcast = podcastService.findById(podcastId) ?: return ResponseEntity.notFound().build<Any>()
        if (podcast.userId != userId) return ResponseEntity.notFound().build<Any>()

        val emitter = SseEmitter(300_000L)

        emitter.onCompletion { log.debug("Preview SSE completed for podcast {}", podcastId) }
        emitter.onTimeout { log.warn("Preview SSE timed out for podcast {}", podcastId) }
        emitter.onError { e -> log.error("Preview SSE error for podcast {}: {}", podcastId, e.message) }

        log.info("Preview SSE requested for podcast {}", podcastId)

        previewScope.launch {
            try {
                val result = podcastService.previewBriefing(podcast) { stage, detail ->
                    try {
                        emitter.send(
                            SseEmitter.event()
                                .name("progress")
                                .data(objectMapper.writeValueAsString(mapOf("stage" to stage) + detail))
                        )
                    } catch (e: Exception) {
                        log.debug("Failed to send progress event: {}", e.message)
                    }
                }

                if (result != null) {
                    emitter.send(
                        SseEmitter.event()
                            .name("result")
                            .data(objectMapper.writeValueAsString(mapOf(
                                "scriptText" to result.script,
                                "style" to podcast.style.value,
                                "articleIds" to result.articleIds
                            )))
                    )
                } else {
                    emitter.send(
                        SseEmitter.event()
                            .name("result")
                            .data(objectMapper.writeValueAsString(mapOf(
                                "message" to "No relevant articles available for preview"
                            )))
                    )
                }

                emitter.send(SseEmitter.event().name("complete").data(""))
                emitter.complete()
            } catch (e: Exception) {
                log.error("Preview pipeline failed for podcast {}: {}", podcastId, e.message, e)
                try {
                    emitter.send(
                        SseEmitter.event()
                            .name("error")
                            .data(objectMapper.writeValueAsString(mapOf("message" to (e.message ?: "Preview generation failed"))))
                    )
                    emitter.complete()
                } catch (sendError: Exception) {
                    emitter.completeWithError(e)
                }
            }
        }

        return emitter
    }

    private fun Podcast.toResponse() = PodcastResponse(
        id = id, userId = userId, name = name, topic = topic,
        language = language, llmModels = llmModels, ttsProvider = ttsProvider.value, ttsVoices = ttsVoices,
        ttsSettings = ttsSettings,
        style = style.value, targetWords = targetWords, cron = cron,
        customInstructions = customInstructions, relevanceThreshold = relevanceThreshold,
        requireReview = requireReview, maxLlmCostCents = maxLlmCostCents,
        maxArticleAgeDays = maxArticleAgeDays, speakerNames = speakerNames,
        fullBodyThreshold = fullBodyThreshold, sponsor = sponsor, pronunciations = pronunciations,
        lastGeneratedAt = lastGeneratedAt
    )
}

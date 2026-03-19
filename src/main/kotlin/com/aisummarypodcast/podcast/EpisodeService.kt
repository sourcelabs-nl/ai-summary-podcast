package com.aisummarypodcast.podcast

import com.aisummarypodcast.llm.ArticleEligibilityService
import com.aisummarypodcast.llm.EpisodeRecapGenerator
import com.aisummarypodcast.llm.ModelResolver
import com.aisummarypodcast.llm.PipelineResult
import com.aisummarypodcast.llm.PipelineStage
import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.EpisodeArticle
import com.aisummarypodcast.store.EpisodeArticleRepository
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.store.EpisodeStatus
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PodcastRepository
import com.aisummarypodcast.store.PostArticleRepository
import com.aisummarypodcast.tts.TtsPipeline
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class EpisodeService(
    private val episodeRepository: EpisodeRepository,
    private val podcastRepository: PodcastRepository,
    private val ttsPipeline: TtsPipeline,
    private val episodeArticleRepository: EpisodeArticleRepository,
    private val articleRepository: ArticleRepository,
    private val episodeRecapGenerator: EpisodeRecapGenerator,
    private val modelResolver: ModelResolver,
    private val postArticleRepository: PostArticleRepository,
    private val episodeSourcesGenerator: EpisodeSourcesGenerator,
    private val articleEligibilityService: ArticleEligibilityService,
    private val eventPublisher: ApplicationEventPublisher
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun createEpisodeFromPipelineResult(
        podcast: Podcast,
        result: PipelineResult,
        overrideGeneratedAt: String? = null,
        updateLastGenerated: Boolean = true
    ): Episode {
        val generatedAt = overrideGeneratedAt ?: Instant.now().toString()
        val episode = if (podcast.requireReview) {
            episodeRepository.save(
                Episode(
                    podcastId = podcast.id,
                    generatedAt = generatedAt,
                    scriptText = result.script,
                    status = EpisodeStatus.PENDING_REVIEW,
                    filterModel = result.filterModel,
                    composeModel = result.composeModel,
                    llmInputTokens = result.llmInputTokens,
                    llmOutputTokens = result.llmOutputTokens,
                    llmCostCents = result.llmCostCents
                )
            )
        } else {
            val ttsEpisode = ttsPipeline.generate(result.script, podcast)
            episodeRepository.save(
                ttsEpisode.copy(
                    filterModel = result.filterModel,
                    composeModel = result.composeModel,
                    llmInputTokens = result.llmInputTokens,
                    llmOutputTokens = result.llmOutputTokens,
                    llmCostCents = result.llmCostCents
                )
            )
        }

        saveEpisodeArticleLinks(episode, result)
        markArticlesAsProcessed(result.processedArticleIds)
        val recapEpisode = generateAndStoreRecap(episode, podcast)
        val finalEpisode = generateAndStoreShowNotes(recapEpisode)
        generateSourcesFile(finalEpisode, podcast)
        if (updateLastGenerated) {
            podcastRepository.save(podcast.copy(lastGeneratedAt = Instant.now().toString()))
        }

        val eventName = if (podcast.requireReview) "episode.created" else "episode.generated"
        eventPublisher.publishEvent(
            PodcastEvent(this, podcast.id, "episode", finalEpisode.id!!, eventName,
                mapOf("episodeNumber" to finalEpisode.id))
        )

        return finalEpisode
    }

    private fun saveEpisodeArticleLinks(episode: Episode, result: PipelineResult) {
        for (articleId in result.processedArticleIds) {
            episodeArticleRepository.save(EpisodeArticle(episodeId = episode.id!!, articleId = articleId))
        }
    }

    private fun markArticlesAsProcessed(articleIds: List<Long>) {
        for (articleId in articleIds) {
            articleRepository.findById(articleId).ifPresent { article ->
                articleRepository.save(article.copy(isProcessed = true))
            }
        }
    }

    private fun generateAndStoreShowNotes(episode: Episode): Episode {
        val showNotes = buildShowNotes(episode.recap) ?: return episode
        val updated = episodeRepository.save(episode.copy(showNotes = showNotes))
        log.info("[Pipeline] Show notes generated for episode {}", episode.id)
        return updated
    }

    private fun buildShowNotes(recap: String?): String? {
        return recap
    }

    private fun generateSourcesFile(episode: Episode, podcast: Podcast) {
        try {
            val links = episodeArticleRepository.findByEpisodeId(episode.id!!)
            val articles = links.mapNotNull { link -> articleRepository.findById(link.articleId).orElse(null) }
                .sortedByDescending { it.relevanceScore ?: 0 }
            episodeSourcesGenerator.generate(episode, podcast, articles)
        } catch (e: Exception) {
            log.warn("Failed to generate sources.md for episode {}: {}", episode.id, e.message)
        }
    }

    private fun generateAndStoreRecap(episode: Episode, podcast: Podcast): Episode {
        return try {
            val filterModelDef = modelResolver.resolve(podcast, PipelineStage.FILTER)
            val recapResult = episodeRecapGenerator.generate(episode.scriptText, podcast, filterModelDef)
            val updated = episodeRepository.save(
                episode.copy(
                    recap = recapResult.recap,
                    llmInputTokens = (episode.llmInputTokens ?: 0) + recapResult.usage.inputTokens,
                    llmOutputTokens = (episode.llmOutputTokens ?: 0) + recapResult.usage.outputTokens
                )
            )
            log.info("[Pipeline] Recap generated and stored for episode {} (podcast '{}' ({}))", episode.id, podcast.name, podcast.id)
            updated
        } catch (e: Exception) {
            log.warn("[Pipeline] Failed to generate recap for episode {} (podcast '{}' ({})) — continuing without recap: {} {}", episode.id, podcast.name, podcast.id, e.javaClass.simpleName, e.message)
            episode
        }
    }

    @Transactional
    fun discardAndResetArticles(episode: Episode, podcastId: String) {
        episodeRepository.save(episode.copy(status = EpisodeStatus.DISCARDED))
        eventPublisher.publishEvent(
            PodcastEvent(this, podcastId, "episode", episode.id!!, "episode.discarded",
                mapOf("episodeNumber" to episode.id))
        )

        val linkedArticles = episodeArticleRepository.findByEpisodeId(episode.id!!)
        if (linkedArticles.isEmpty()) {
            log.warn("Episode {} has no episode-article links; cannot reset articles for reprocessing", episode.id)
            return
        }

        var resetCount = 0
        var deletedCount = 0
        var skippedCount = 0
        for (link in linkedArticles) {
            articleRepository.findById(link.articleId).ifPresent { article ->
                if (!articleEligibilityService.canResetArticle(article.id!!)) {
                    skippedCount++
                    return@ifPresent
                }
                val postCount = postArticleRepository.countByArticleId(article.id)
                if (postCount >= 2) {
                    postArticleRepository.deleteByArticleId(article.id)
                    articleRepository.deleteById(article.id)
                    deletedCount++
                } else {
                    articleRepository.save(article.copy(isProcessed = false))
                    resetCount++
                }
            }
        }

        // Roll back lastGeneratedAt so reset articles appear in upcoming again
        if (resetCount > 0) {
            val podcast = podcastRepository.findById(podcastId).orElse(null)
            if (podcast != null) {
                val lastPublished = episodeRepository.findLatestPublishedByPodcastId(podcastId)
                val rollbackTo = lastPublished?.generatedAt
                podcastRepository.save(podcast.copy(lastGeneratedAt = rollbackTo))
                log.info("Episode {} discard: rolled back lastGeneratedAt to {} for podcast '{}'", episode.id, rollbackTo ?: "null", podcast.name)
            }
        }

        log.info("Episode {} discarded, reset {} articles, deleted {} aggregated articles, skipped {} (linked to published episodes)", episode.id, resetCount, deletedCount, skippedCount)
    }

    fun updateScript(episode: Episode, scriptText: String): Episode {
        return episodeRepository.save(episode.copy(scriptText = scriptText))
    }

    fun approveAndGenerateAudio(episode: Episode, podcast: Podcast) {
        episodeRepository.save(episode.copy(status = EpisodeStatus.APPROVED, errorMessage = null))
        log.info("Episode {} approved, triggering async TTS generation", episode.id)
        eventPublisher.publishEvent(
            PodcastEvent(this, podcast.id, "episode", episode.id!!, "episode.approved",
                mapOf("episodeNumber" to episode.id))
        )
        generateAudioAsync(episode.id, podcast.id)
    }

    fun createFailedEpisode(podcast: Podcast, errorMessage: String): Episode {
        val episode = episodeRepository.save(
            Episode(
                podcastId = podcast.id,
                generatedAt = Instant.now().toString(),
                scriptText = "",
                status = EpisodeStatus.FAILED,
                errorMessage = errorMessage
            )
        )
        podcastRepository.save(podcast.copy(lastGeneratedAt = Instant.now().toString()))
        eventPublisher.publishEvent(
            PodcastEvent(this, podcast.id, "episode", episode.id!!, "episode.failed",
                mapOf("episodeNumber" to episode.id, "error" to errorMessage))
        )
        log.info("[Pipeline] Created FAILED episode {} for podcast '{}' ({}): {}", episode.id, podcast.name, podcast.id, errorMessage)
        return episode
    }

    fun regenerateRecap(episode: Episode, podcast: Podcast): Episode {
        val recapEpisode = generateAndStoreRecap(episode, podcast)
        val finalEpisode = generateAndStoreShowNotes(recapEpisode)
        generateSourcesFile(finalEpisode, podcast)
        return finalEpisode
    }

    fun findById(episodeId: Long): Episode? = episodeRepository.findById(episodeId).orElse(null)

    fun hasPendingOrApprovedEpisode(podcastId: String): Boolean {
        return episodeRepository.findByPodcastIdAndStatusIn(
            podcastId, listOf(EpisodeStatus.PENDING_REVIEW.name, EpisodeStatus.APPROVED.name)
        ).isNotEmpty()
    }

    @Async
    fun generateAudioAsync(episodeId: Long, podcastId: String) {
        val episode = episodeRepository.findById(episodeId).orElse(null)
        if (episode == null) {
            log.error("Episode {} not found for async TTS generation", episodeId)
            return
        }

        val podcast = podcastRepository.findById(podcastId).orElse(null)
        if (podcast == null) {
            log.error("Podcast {} not found for async TTS generation", podcastId)
            episodeRepository.save(episode.copy(status = EpisodeStatus.FAILED, errorMessage = "Podcast not found"))
            return
        }

        try {
            log.info("Starting async TTS generation for episode {} (podcast '{}' ({}))", episodeId, podcast.name, podcastId)
            eventPublisher.publishEvent(
                PodcastEvent(this, podcastId, "episode", episodeId, "episode.audio.started",
                    mapOf("episodeNumber" to episodeId))
            )
            ttsPipeline.generateForExistingEpisode(episode, podcast)
            log.info("Async TTS generation complete for episode {} (podcast '{}' ({}))", episodeId, podcast.name, podcastId)
            eventPublisher.publishEvent(
                PodcastEvent(this, podcastId, "episode", episodeId, "episode.generated",
                    mapOf("episodeNumber" to episodeId))
            )
        } catch (e: Exception) {
            log.error("Async TTS generation failed for episode {} (podcast '{}' ({})): {}", episodeId, podcast.name, podcastId, e.message, e)
            episodeRepository.save(episode.copy(status = EpisodeStatus.FAILED, errorMessage = e.message))
            eventPublisher.publishEvent(
                PodcastEvent(this, podcastId, "episode", episodeId, "episode.failed",
                    mapOf("episodeNumber" to episodeId, "error" to (e.message ?: "Unknown error")))
            )
        }
    }
}

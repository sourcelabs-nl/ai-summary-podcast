package com.aisummarypodcast.podcast

import com.aisummarypodcast.llm.EpisodeRecapGenerator
import com.aisummarypodcast.llm.ModelResolver
import com.aisummarypodcast.llm.PipelineResult
import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.EpisodeArticle
import com.aisummarypodcast.store.EpisodeArticleRepository
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PodcastRepository
import com.aisummarypodcast.tts.TtsPipeline
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class EpisodeService(
    private val episodeRepository: EpisodeRepository,
    private val podcastRepository: PodcastRepository,
    private val ttsPipeline: TtsPipeline,
    private val episodeArticleRepository: EpisodeArticleRepository,
    private val episodeRecapGenerator: EpisodeRecapGenerator,
    private val modelResolver: ModelResolver
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun createEpisodeFromPipelineResult(podcast: Podcast, result: PipelineResult): Episode {
        val episode = if (podcast.requireReview) {
            episodeRepository.save(
                Episode(
                    podcastId = podcast.id,
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
        generateAndStoreRecap(episode, podcast)
        podcastRepository.save(podcast.copy(lastGeneratedAt = Instant.now().toString()))

        return episode
    }

    private fun saveEpisodeArticleLinks(episode: Episode, result: PipelineResult) {
        for (articleId in result.processedArticleIds) {
            episodeArticleRepository.save(EpisodeArticle(episodeId = episode.id!!, articleId = articleId))
        }
    }

    private fun generateAndStoreRecap(episode: Episode, podcast: Podcast) {
        try {
            val filterModelDef = modelResolver.resolve(podcast, "filter")
            val recapResult = episodeRecapGenerator.generate(episode.scriptText, podcast, filterModelDef)
            episodeRepository.save(
                episode.copy(
                    recap = recapResult.recap,
                    llmInputTokens = (episode.llmInputTokens ?: 0) + recapResult.usage.inputTokens,
                    llmOutputTokens = (episode.llmOutputTokens ?: 0) + recapResult.usage.outputTokens
                )
            )
            log.info("[Pipeline] Recap generated and stored for episode {}", episode.id)
        } catch (e: Exception) {
            log.warn("[Pipeline] Failed to generate recap for episode {} — continuing without recap: {}", episode.id, e.message)
        }
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
            episodeRepository.save(episode.copy(status = "FAILED"))
            return
        }

        try {
            log.info("Starting async TTS generation for episode {} (podcast {})", episodeId, podcastId)
            val generatedEpisode = ttsPipeline.generateForExistingEpisode(episode, podcast)
            log.info("Async TTS generation complete for episode {} (podcast {})", episodeId, podcastId)
        } catch (e: Exception) {
            log.error("Async TTS generation failed for episode {}: {}", episodeId, e.message, e)
            episodeRepository.save(episode.copy(status = "FAILED"))
        }
    }
}

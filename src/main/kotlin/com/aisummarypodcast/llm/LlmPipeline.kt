package com.aisummarypodcast.llm

import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.SourceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import kotlin.time.measureTimedValue

data class PipelineResult(
    val script: String,
    val filterModel: String,
    val composeModel: String
)

@Component
class LlmPipeline(
    private val articleProcessor: ArticleProcessor,
    private val briefingComposer: BriefingComposer,
    private val modelResolver: ModelResolver,
    private val articleRepository: ArticleRepository,
    private val sourceRepository: SourceRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun run(podcast: Podcast): PipelineResult? {
        val sourceIds = sourceRepository.findByPodcastId(podcast.id).map { it.id }
        if (sourceIds.isEmpty()) {
            log.info("[LLM] Podcast {} has no sources — skipping", podcast.id)
            return null
        }

        val unfiltered = articleRepository.findUnfilteredBySourceIds(sourceIds)
        if (unfiltered.isEmpty()) {
            log.info("[LLM] No unfiltered articles for podcast {} — skipping briefing generation", podcast.id)
            return null
        }

        val filterModelDef = modelResolver.resolve(podcast, "filter")
        val composeModelDef = modelResolver.resolve(podcast, "compose")

        log.info("[LLM] Processing {} articles for podcast {}", unfiltered.size, podcast.id)
        val (processed, articleDuration) = measureTimedValue {
            articleProcessor.process(unfiltered, podcast, filterModelDef)
        }
        val relevantCount = processed.size
        log.info("[LLM] Article processing complete — {} articles in {} ({} relevant)", unfiltered.size, articleDuration, relevantCount)

        if (processed.isEmpty()) {
            log.info("[LLM] No relevant articles for podcast {} — skipping briefing generation", podcast.id)
            return null
        }

        val script = briefingComposer.compose(processed, podcast, composeModelDef)

        for (article in processed) {
            articleRepository.save(article.copy(isProcessed = true))
        }

        log.info("[LLM] Pipeline complete for podcast {}: {} articles processed into briefing", podcast.id, processed.size)
        return PipelineResult(
            script = script,
            filterModel = filterModelDef.model,
            composeModel = composeModelDef.model
        )
    }
}

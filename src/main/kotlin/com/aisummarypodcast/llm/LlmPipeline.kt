package com.aisummarypodcast.llm

import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.SourceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class LlmPipeline(
    private val articleProcessor: ArticleProcessor,
    private val briefingComposer: BriefingComposer,
    private val articleRepository: ArticleRepository,
    private val sourceRepository: SourceRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun run(podcast: Podcast): String? {
        val sourceIds = sourceRepository.findByPodcastId(podcast.id).map { it.id }
        if (sourceIds.isEmpty()) {
            log.info("Podcast {} has no sources — skipping", podcast.id)
            return null
        }

        val unfiltered = articleRepository.findUnfilteredBySourceIds(sourceIds)
        if (unfiltered.isEmpty()) {
            log.info("No unfiltered articles for podcast {} — skipping briefing generation", podcast.id)
            return null
        }

        log.info("Processing {} articles for podcast {}", unfiltered.size, podcast.id)
        val processed = articleProcessor.process(unfiltered, podcast)

        if (processed.isEmpty()) {
            log.info("No relevant articles for podcast {} — skipping briefing generation", podcast.id)
            return null
        }

        log.info("Composing briefing script for podcast {}", podcast.id)
        val script = briefingComposer.compose(processed, podcast)

        for (article in processed) {
            articleRepository.save(article.copy(isProcessed = true))
        }

        log.info("LLM pipeline complete for podcast {}: {} articles processed into briefing", podcast.id, processed.size)
        return script
    }
}

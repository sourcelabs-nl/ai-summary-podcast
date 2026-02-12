package com.aisummarypodcast.llm

import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.SourceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class LlmPipeline(
    private val relevanceFilter: RelevanceFilter,
    private val articleSummarizer: ArticleSummarizer,
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
            log.info("No unfiltered articles for podcast {}", podcast.id)
        } else {
            log.info("Filtering {} articles for relevance (podcast {})", unfiltered.size, podcast.id)
            relevanceFilter.filter(unfiltered, podcast)
        }

        val relevant = articleRepository.findRelevantUnprocessedBySourceIds(sourceIds)
        if (relevant.isEmpty()) {
            log.info("No relevant unprocessed articles for podcast {} — skipping briefing generation", podcast.id)
            return null
        }

        log.info("Summarizing {} relevant articles for podcast {}", relevant.size, podcast.id)
        val summarized = articleSummarizer.summarize(relevant, podcast)

        log.info("Composing briefing script for podcast {}", podcast.id)
        val script = briefingComposer.compose(summarized, podcast)

        for (article in relevant) {
            articleRepository.save(article.copy(isProcessed = true))
        }

        log.info("LLM pipeline complete for podcast {}: {} articles processed into briefing", podcast.id, relevant.size)
        return script
    }
}

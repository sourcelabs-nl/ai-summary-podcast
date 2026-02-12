package com.aisummarypodcast.llm

import com.aisummarypodcast.store.ArticleRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class LlmPipeline(
    private val relevanceFilter: RelevanceFilter,
    private val articleSummarizer: ArticleSummarizer,
    private val briefingComposer: BriefingComposer,
    private val articleRepository: ArticleRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun run(): String? {
        val unfiltered = articleRepository.findUnfiltered()
        if (unfiltered.isEmpty()) {
            log.info("No unfiltered articles to process")
        } else {
            log.info("Filtering {} articles for relevance", unfiltered.size)
            relevanceFilter.filter(unfiltered)
        }

        val relevant = articleRepository.findRelevantUnprocessed()
        if (relevant.isEmpty()) {
            log.info("No relevant unprocessed articles — skipping briefing generation")
            return null
        }

        log.info("Summarizing {} relevant articles", relevant.size)
        val summarized = articleSummarizer.summarize(relevant)

        log.info("Composing briefing script")
        val script = briefingComposer.compose(summarized)

        // Mark all processed articles
        for (article in relevant) {
            articleRepository.save(article.copy(isProcessed = true))
        }

        log.info("LLM pipeline complete: {} articles processed into briefing", relevant.size)
        return script
    }
}

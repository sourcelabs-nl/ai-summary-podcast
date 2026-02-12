package com.aisummarypodcast.podcast

import com.aisummarypodcast.llm.LlmPipeline
import com.aisummarypodcast.tts.TtsPipeline
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class FeedController(
    private val feedGenerator: FeedGenerator,
    private val llmPipeline: LlmPipeline,
    private val ttsPipeline: TtsPipeline
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/feed.xml", produces = ["application/rss+xml"])
    fun feed(): String = feedGenerator.generate()

    @PostMapping("/generate")
    fun generate(): ResponseEntity<String> {
        log.info("Manual briefing generation triggered")

        val script = llmPipeline.run()
            ?: return ResponseEntity.ok("No relevant articles to process")

        val episode = ttsPipeline.generate(script)
        return ResponseEntity.ok("Episode generated: ${episode.id} (${episode.durationSeconds}s)")
    }
}

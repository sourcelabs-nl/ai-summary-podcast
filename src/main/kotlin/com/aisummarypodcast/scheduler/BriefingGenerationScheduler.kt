package com.aisummarypodcast.scheduler

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.llm.LlmPipeline
import com.aisummarypodcast.tts.TtsPipeline
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class BriefingGenerationScheduler(
    private val llmPipeline: LlmPipeline,
    private val ttsPipeline: TtsPipeline,
    private val appProperties: AppProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${app.briefing.cron}")
    fun generateBriefing() {
        log.info("Starting briefing generation pipeline")

        val script = llmPipeline.run()
        if (script == null) {
            log.info("No briefing script generated — nothing to synthesize")
            return
        }

        val episode = ttsPipeline.generate(script)
        log.info("Briefing generation complete: episode {} ({} seconds)", episode.id, episode.durationSeconds)
    }
}

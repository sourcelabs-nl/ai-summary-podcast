package com.aisummarypodcast.config

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class PodcastDefaultsResponse(
    val llmModels: Map<String, String>,
    val maxLlmCostCents: Int,
    val targetWords: Int,
    val fullBodyThreshold: Int,
    val maxArticleAgeDays: Int
)

@RestController
@RequestMapping("/config")
class ConfigController(private val appProperties: AppProperties) {

    @GetMapping("/defaults")
    fun defaults(): PodcastDefaultsResponse {
        val resolvedModels = appProperties.llm.defaults.let { defaults ->
            mapOf(
                "filter" to (appProperties.llm.models[defaults.filter]?.model ?: defaults.filter),
                "compose" to (appProperties.llm.models[defaults.compose]?.model ?: defaults.compose)
            )
        }
        return PodcastDefaultsResponse(
            llmModels = resolvedModels,
            maxLlmCostCents = appProperties.llm.maxCostCents,
            targetWords = appProperties.briefing.targetWords,
            fullBodyThreshold = appProperties.briefing.fullBodyThreshold,
            maxArticleAgeDays = appProperties.source.maxArticleAgeDays
        )
    }
}

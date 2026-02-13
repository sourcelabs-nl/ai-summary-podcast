package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.ModelDefinition
import com.aisummarypodcast.store.Podcast
import org.springframework.stereotype.Component

@Component
class ModelResolver(
    private val appProperties: AppProperties
) {

    fun resolve(podcast: Podcast, stage: String): ModelDefinition {
        val modelName = podcast.llmModels?.get(stage)
            ?: getDefaultForStage(stage)

        return appProperties.llm.models[modelName]
            ?: throw IllegalArgumentException(
                "Unknown model name '$modelName'. Available models: ${appProperties.llm.models.keys.sorted()}"
            )
    }

    private fun getDefaultForStage(stage: String): String = when (stage) {
        "filter" -> appProperties.llm.defaults.filter
        "compose" -> appProperties.llm.defaults.compose
        else -> throw IllegalArgumentException("Unknown pipeline stage '$stage'")
    }
}

package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.ModelDefinition
import com.aisummarypodcast.store.Podcast
import org.springframework.stereotype.Component

@Component
class ModelResolver(
    private val appProperties: AppProperties
) {

    fun resolve(podcast: Podcast, stage: PipelineStage): ModelDefinition {
        val modelName = podcast.llmModels?.get(stage.value)
            ?: getDefaultForStage(stage)

        return appProperties.llm.models[modelName]
            ?: throw IllegalArgumentException(
                "Unknown model name '$modelName'. Available models: ${appProperties.llm.models.keys.sorted()}"
            )
    }

    private fun getDefaultForStage(stage: PipelineStage): String = when (stage) {
        PipelineStage.FILTER -> appProperties.llm.defaults.filter
        PipelineStage.COMPOSE -> appProperties.llm.defaults.compose
    }
}

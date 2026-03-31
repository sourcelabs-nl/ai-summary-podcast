package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.ModelCost
import com.aisummarypodcast.config.ModelReference
import com.aisummarypodcast.store.Podcast
import org.springframework.stereotype.Component

data class ResolvedModel(
    val provider: String,
    val model: String,
    val cost: ModelCost?
)

@Component
class ModelResolver(
    private val appProperties: AppProperties
) {

    fun resolve(podcast: Podcast, stage: PipelineStage): ResolvedModel {
        val ref = podcast.llmModels?.get(stage.value)
            ?: stage.default(appProperties.llm.defaults)

        val cost = appProperties.models[ref.provider]?.get(ref.model)

        return ResolvedModel(
            provider = ref.provider,
            model = ref.model,
            cost = cost
        )
    }
}

package com.aisummarypodcast.llm

import com.aisummarypodcast.config.ModelReference
import com.aisummarypodcast.config.StageDefaults

enum class PipelineStage(val value: String) {
    FILTER("filter"),
    COMPOSE("compose");

    fun default(defaults: StageDefaults): ModelReference = when (this) {
        FILTER -> defaults.filter
        COMPOSE -> defaults.compose
    }
}

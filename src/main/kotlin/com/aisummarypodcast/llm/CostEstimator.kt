package com.aisummarypodcast.llm

import com.aisummarypodcast.config.ModelCost
import com.aisummarypodcast.store.Article
import kotlin.math.roundToInt

object CostEstimator {

    fun estimateLlmCostCents(inputTokens: Int, outputTokens: Int, cost: ModelCost?): Int? {
        val inputCost = cost?.inputCostPerMtok ?: return null
        val outputCost = cost.outputCostPerMtok ?: return null
        val costUsd = (inputTokens * inputCost + outputTokens * outputCost) / 1_000_000.0
        return (costUsd * 100).roundToInt()
    }

    fun estimateTtsCostCents(characters: Int, models: Map<String, Map<String, ModelCost>>, provider: String, model: String? = null): Int? {
        val providerModels = models[provider] ?: return null
        val cost = (model?.let { providerModels[it] } ?: providerModels.values.firstOrNull())
            ?: return null
        val rate = cost.costPerMillionChars ?: return null
        val costUsd = characters * rate / 1_000_000.0
        return (costUsd * 100).roundToInt()
    }

    fun estimatePipelineCostCents(
        articles: List<Article>,
        filterModel: ResolvedModel,
        composeModel: ResolvedModel,
        targetWords: Int
    ): Int? {
        val scoringInputTokens = articles.sumOf { it.body.length / 4 }
        val scoringOutputTokens = 200 * articles.size
        val scoringCost = estimateLlmCostCents(scoringInputTokens, scoringOutputTokens, filterModel.cost)

        val compositionInputTokens = articles.size * 200
        val compositionOutputTokens = (targetWords * 1.3).roundToInt()
        val compositionCost = estimateLlmCostCents(compositionInputTokens, compositionOutputTokens, composeModel.cost)

        if (scoringCost == null || compositionCost == null) return null
        return scoringCost + compositionCost
    }

    fun addNullableCosts(existing: Int?, additional: Int?): Int? {
        if (existing == null && additional == null) return null
        return (existing ?: 0) + (additional ?: 0)
    }
}

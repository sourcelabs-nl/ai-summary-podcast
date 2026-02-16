package com.aisummarypodcast.llm

import com.aisummarypodcast.config.ModelDefinition
import com.aisummarypodcast.store.Article
import kotlin.math.roundToInt

object CostEstimator {

    fun estimateLlmCostCents(inputTokens: Int, outputTokens: Int, modelDef: ModelDefinition): Int? {
        val inputCost = modelDef.inputCostPerMtok ?: return null
        val outputCost = modelDef.outputCostPerMtok ?: return null
        val costUsd = (inputTokens * inputCost + outputTokens * outputCost) / 1_000_000.0
        return (costUsd * 100).roundToInt()
    }

    fun estimateTtsCostCents(characters: Int, costPerMillionChars: Double?): Int? {
        costPerMillionChars ?: return null
        val costUsd = characters * costPerMillionChars / 1_000_000.0
        return (costUsd * 100).roundToInt()
    }

    fun estimatePipelineCostCents(
        articles: List<Article>,
        filterModelDef: ModelDefinition,
        composeModelDef: ModelDefinition,
        targetWords: Int
    ): Int? {
        // Scoring stage: one call per article using the filter model
        val scoringInputTokens = articles.sumOf { it.body.length / 4 }
        val scoringOutputTokens = 200 * articles.size

        val scoringCost = estimateLlmCostCents(scoringInputTokens, scoringOutputTokens, filterModelDef)

        // Composition stage: assumes all articles pass relevance filtering (pessimistic)
        // Input: N articles × 200 tokens per summary
        val compositionInputTokens = articles.size * 200
        // Output: targetWords converted to tokens
        val compositionOutputTokens = (targetWords * 1.3).roundToInt()

        val compositionCost = estimateLlmCostCents(compositionInputTokens, compositionOutputTokens, composeModelDef)

        if (scoringCost == null || compositionCost == null) return null
        return scoringCost + compositionCost
    }

    fun addNullableCosts(existing: Int?, additional: Int?): Int? {
        if (existing == null && additional == null) return null
        return (existing ?: 0) + (additional ?: 0)
    }
}

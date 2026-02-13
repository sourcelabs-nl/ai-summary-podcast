package com.aisummarypodcast.llm

import com.aisummarypodcast.config.ModelDefinition
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

    fun addNullableCosts(existing: Int?, additional: Int?): Int? {
        if (existing == null && additional == null) return null
        return (existing ?: 0) + (additional ?: 0)
    }
}

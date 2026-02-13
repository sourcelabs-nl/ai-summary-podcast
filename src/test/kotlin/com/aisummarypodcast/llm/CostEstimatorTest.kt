package com.aisummarypodcast.llm

import com.aisummarypodcast.config.ModelDefinition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CostEstimatorTest {

    @Test
    fun `estimates LLM cost with configured pricing`() {
        val modelDef = ModelDefinition(
            provider = "openrouter", model = "test",
            inputCostPerMtok = 3.00, outputCostPerMtok = 15.00
        )
        // (10000 * 3.00 + 2000 * 15.00) / 1_000_000 = 0.06 USD = 6 cents
        val cost = CostEstimator.estimateLlmCostCents(10000, 2000, modelDef)
        assertEquals(6, cost)
    }

    @Test
    fun `returns null when pricing not configured`() {
        val modelDef = ModelDefinition(provider = "openrouter", model = "test")
        assertNull(CostEstimator.estimateLlmCostCents(1000, 200, modelDef))
    }

    @Test
    fun `returns null when only input pricing configured`() {
        val modelDef = ModelDefinition(
            provider = "openrouter", model = "test",
            inputCostPerMtok = 3.00, outputCostPerMtok = null
        )
        assertNull(CostEstimator.estimateLlmCostCents(1000, 200, modelDef))
    }

    @Test
    fun `rounds small costs to nearest cent`() {
        val modelDef = ModelDefinition(
            provider = "openrouter", model = "test",
            inputCostPerMtok = 0.15, outputCostPerMtok = 0.60
        )
        // (1000 * 0.15 + 200 * 0.60) / 1_000_000 = 0.00027 USD = 0.027 cents → 0
        val cost = CostEstimator.estimateLlmCostCents(1000, 200, modelDef)
        assertEquals(0, cost)
    }

    @Test
    fun `estimates TTS cost`() {
        // 50000 * 15.00 / 1_000_000 = 0.75 USD = 75 cents... wait
        // Actually: 50000 * 15.00 / 1_000_000 = 0.75 USD → 0.75 * 100 = 75 cents
        val cost = CostEstimator.estimateTtsCostCents(50000, 15.00)
        assertEquals(75, cost)
    }

    @Test
    fun `returns null when TTS pricing not configured`() {
        assertNull(CostEstimator.estimateTtsCostCents(8000, null))
    }

    @Test
    fun `handles zero tokens`() {
        val modelDef = ModelDefinition(
            provider = "openrouter", model = "test",
            inputCostPerMtok = 3.00, outputCostPerMtok = 15.00
        )
        assertEquals(0, CostEstimator.estimateLlmCostCents(0, 0, modelDef))
    }
}

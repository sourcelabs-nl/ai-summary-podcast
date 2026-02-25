package com.aisummarypodcast.llm

import com.aisummarypodcast.config.ModelDefinition
import com.aisummarypodcast.store.Article
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
        val cost = CostEstimator.estimateTtsCostCents(50000, mapOf("openai" to 15.00), "openai")
        assertEquals(75, cost)
    }

    @Test
    fun `returns null when TTS pricing not configured for provider`() {
        assertNull(CostEstimator.estimateTtsCostCents(8000, mapOf("openai" to 15.00), "elevenlabs"))
    }

    @Test
    fun `returns null when TTS pricing map is empty`() {
        assertNull(CostEstimator.estimateTtsCostCents(8000, emptyMap(), "openai"))
    }

    @Test
    fun `estimates ElevenLabs TTS cost`() {
        val cost = CostEstimator.estimateTtsCostCents(8000, mapOf("elevenlabs" to 30.00), "elevenlabs")
        assertEquals(24, cost)
    }

    @Test
    fun `estimates Inworld TTS Max cost by model name`() {
        val pricing = mapOf("inworld-tts-1.5-max" to 10.00, "inworld-tts-1.5-mini" to 5.00)
        // 8000 * 10.00 / 1_000_000 * 100 = 8 cents
        val cost = CostEstimator.estimateTtsCostCents(8000, pricing, "inworld", "inworld-tts-1.5-max")
        assertEquals(8, cost)
    }

    @Test
    fun `estimates Inworld TTS Mini cost by model name`() {
        val pricing = mapOf("inworld-tts-1.5-max" to 10.00, "inworld-tts-1.5-mini" to 5.00)
        // 8000 * 5.00 / 1_000_000 * 100 = 4 cents
        val cost = CostEstimator.estimateTtsCostCents(8000, pricing, "inworld", "inworld-tts-1.5-mini")
        assertEquals(4, cost)
    }

    @Test
    fun `returns null when Inworld model pricing not configured`() {
        val pricing = mapOf("openai" to 15.00)
        assertNull(CostEstimator.estimateTtsCostCents(8000, pricing, "inworld", "inworld-tts-1.5-max"))
    }

    @Test
    fun `falls back to provider name when model not in pricing map`() {
        val pricing = mapOf("openai" to 15.00)
        // model "tts-1" not in map, falls back to "openai"
        val cost = CostEstimator.estimateTtsCostCents(50000, pricing, "openai", "tts-1")
        assertEquals(75, cost)
    }

    @Test
    fun `handles zero tokens`() {
        val modelDef = ModelDefinition(
            provider = "openrouter", model = "test",
            inputCostPerMtok = 3.00, outputCostPerMtok = 15.00
        )
        assertEquals(0, CostEstimator.estimateLlmCostCents(0, 0, modelDef))
    }

    // --- estimatePipelineCostCents tests ---

    private val cheapModel = ModelDefinition(
        provider = "openrouter", model = "gpt-4o-mini",
        inputCostPerMtok = 0.15, outputCostPerMtok = 0.60
    )

    private val capableModel = ModelDefinition(
        provider = "openrouter", model = "claude-sonnet",
        inputCostPerMtok = 3.00, outputCostPerMtok = 15.00
    )

    private fun article(body: String) = Article(
        sourceId = "src1", title = "Test", body = body, url = "http://test.com", contentHash = "hash"
    )

    @Test
    fun `estimates pipeline cost for articles with default models`() {
        // 10 articles, each 2000 chars
        val articles = (1..10).map { article("x".repeat(2000)) }

        val cost = CostEstimator.estimatePipelineCostCents(articles, cheapModel, capableModel, 1500)

        // Scoring: input = 10 * (2000/4) = 5000 tokens, output = 10 * 200 = 2000 tokens
        // Scoring cost = (5000 * 0.15 + 2000 * 0.60) / 1_000_000 * 100 = 0.195 cents → 0
        // Composition: input = 10 * 200 = 2000 tokens, output = 1500 * 1.3 = 1950 tokens
        // Composition cost = (2000 * 3.00 + 1950 * 15.00) / 1_000_000 * 100 = 3.525 cents → 4
        // Total: 0 + 4 = 4 cents
        assertEquals(4, cost)
    }

    @Test
    fun `estimates pipeline cost with varying article sizes`() {
        val articles = listOf(
            article("x".repeat(500)),
            article("x".repeat(3000)),
            article("x".repeat(8000))
        )

        val cost = CostEstimator.estimatePipelineCostCents(articles, cheapModel, capableModel, 1500)

        // Scoring: input = (500/4) + (3000/4) + (8000/4) = 125 + 750 + 2000 = 2875 tokens
        // Scoring: output = 3 * 200 = 600 tokens
        // Scoring cost = (2875 * 0.15 + 600 * 0.60) / 1_000_000 * 100 ≈ 0.079 cents → 0
        // Composition: input = 3 * 200 = 600 tokens, output = 1950 tokens
        // Composition cost = (600 * 3.00 + 1950 * 15.00) / 1_000_000 * 100 ≈ 3.105 cents → 3
        // Total: 0 + 3 = 3 cents
        assertEquals(3, cost)
    }

    @Test
    fun `returns null when pricing not configured for filter model`() {
        val noPricingModel = ModelDefinition(provider = "openrouter", model = "test")
        val articles = listOf(article("x".repeat(1000)))

        val cost = CostEstimator.estimatePipelineCostCents(articles, noPricingModel, capableModel, 1500)

        assertNull(cost)
    }

    @Test
    fun `returns null when pricing not configured for compose model`() {
        val noPricingModel = ModelDefinition(provider = "openrouter", model = "test")
        val articles = listOf(article("x".repeat(1000)))

        val cost = CostEstimator.estimatePipelineCostCents(articles, cheapModel, noPricingModel, 1500)

        assertNull(cost)
    }

    @Test
    fun `returns null when pricing not configured for both models`() {
        val noPricingModel = ModelDefinition(provider = "openrouter", model = "test")
        val articles = listOf(article("x".repeat(1000)))

        val cost = CostEstimator.estimatePipelineCostCents(articles, noPricingModel, noPricingModel, 1500)

        assertNull(cost)
    }
}

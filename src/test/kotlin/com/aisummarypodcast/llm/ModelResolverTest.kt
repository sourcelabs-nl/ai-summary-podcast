package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.BriefingProperties
import com.aisummarypodcast.config.EncryptionProperties
import com.aisummarypodcast.config.EpisodesProperties
import com.aisummarypodcast.config.FeedProperties
import com.aisummarypodcast.config.LlmProperties
import com.aisummarypodcast.config.ModelDefinition
import com.aisummarypodcast.config.StageDefaults
import com.aisummarypodcast.store.Podcast
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ModelResolverTest {

    private val models = mapOf(
        "cheap" to ModelDefinition(provider = "openrouter", model = "anthropic/claude-haiku-4.5"),
        "capable" to ModelDefinition(provider = "openrouter", model = "anthropic/claude-sonnet-4"),
        "local" to ModelDefinition(provider = "ollama", model = "llama3")
    )

    private val appProperties = AppProperties(
        llm = LlmProperties(
            models = models,
            defaults = StageDefaults(filter = "cheap", compose = "capable")
        ),
        briefing = BriefingProperties(),
        episodes = EpisodesProperties(),
        feed = FeedProperties(),
        encryption = EncryptionProperties(masterKey = "test-key")
    )

    private val resolver = ModelResolver(appProperties)

    private val podcast = Podcast(id = "p1", userId = "u1", name = "Test", topic = "tech")

    @Test
    fun `uses global default when podcast has no overrides`() {
        val filterModel = resolver.resolve(podcast, PipelineStage.FILTER)
        val composeModel = resolver.resolve(podcast, PipelineStage.COMPOSE)

        assertEquals("openrouter", filterModel.provider)
        assertEquals("anthropic/claude-haiku-4.5", filterModel.model)
        assertEquals("openrouter", composeModel.provider)
        assertEquals("anthropic/claude-sonnet-4", composeModel.model)
    }

    @Test
    fun `podcast override takes precedence over global default`() {
        val podcastWithOverride = podcast.copy(llmModels = mapOf("compose" to "local"))

        val composeModel = resolver.resolve(podcastWithOverride, PipelineStage.COMPOSE)

        assertEquals("ollama", composeModel.provider)
        assertEquals("llama3", composeModel.model)
    }

    @Test
    fun `partial override only affects specified stage`() {
        val podcastWithOverride = podcast.copy(llmModels = mapOf("compose" to "local"))

        val filterModel = resolver.resolve(podcastWithOverride, PipelineStage.FILTER)
        val composeModel = resolver.resolve(podcastWithOverride, PipelineStage.COMPOSE)

        assertEquals("anthropic/claude-haiku-4.5", filterModel.model)
        assertEquals("llama3", composeModel.model)
    }

    @Test
    fun `throws for unknown model name`() {
        val podcastWithBadOverride = podcast.copy(llmModels = mapOf("filter" to "nonexistent"))

        val exception = assertThrows<IllegalArgumentException> {
            resolver.resolve(podcastWithBadOverride, PipelineStage.FILTER)
        }

        assertEquals(
            "Unknown model name 'nonexistent'. Available models: [capable, cheap, local]",
            exception.message
        )
    }

}

package com.aisummarypodcast.llm

import com.aisummarypodcast.config.ModelDefinition
import com.aisummarypodcast.store.Podcast
import org.slf4j.LoggerFactory
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.stereotype.Component
import kotlin.time.measureTimedValue

data class RecapResult(
    val recap: String,
    val usage: TokenUsage
)

@Component
class EpisodeRecapGenerator(
    private val chatClientFactory: ChatClientFactory
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun generate(scriptText: String, podcast: Podcast, filterModelDef: ModelDefinition): RecapResult {
        log.info("[LLM] Generating recap of previous episode for podcast {}", podcast.id)
        val chatClient = chatClientFactory.createForModel(podcast.userId, filterModelDef)
        val prompt = buildPrompt(scriptText)

        val (result, elapsed) = measureTimedValue {
            val chatResponse = chatClient.prompt()
                .user(prompt)
                .options(OpenAiChatOptions.builder().model(filterModelDef.model).build())
                .call()
                .chatResponse()

            val recap = chatResponse?.result?.output?.text
                ?: throw IllegalStateException("Empty response from LLM for episode recap generation")

            val usage = TokenUsage.fromChatResponse(chatResponse)
            RecapResult(recap.trim(), usage)
        }

        log.info("[LLM] Episode recap generated in {}", elapsed)
        return result
    }

    internal fun buildPrompt(scriptText: String): String {
        return """
            Summarize the following podcast episode script in 2-3 sentences. Focus on the main topics and key points discussed. Write direct, concise statements without any preamble, meta-commentary, or introductory phrases like "In this episode" or "The podcast covered".

            Episode script:
            $scriptText
        """.trimIndent()
    }
}

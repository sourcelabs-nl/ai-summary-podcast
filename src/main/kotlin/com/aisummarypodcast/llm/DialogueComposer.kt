package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.ModelDefinition
import com.aisummarypodcast.podcast.SupportedLanguage
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.Podcast
import org.slf4j.LoggerFactory
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.stereotype.Component
import java.net.URI
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.measureTimedValue

@Component
class DialogueComposer(
    private val appProperties: AppProperties,
    private val modelResolver: ModelResolver,
    private val chatClientFactory: ChatClientFactory
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun compose(articles: List<Article>, podcast: Podcast): CompositionResult {
        val composeModelDef = modelResolver.resolve(podcast, "compose")
        return compose(articles, podcast, composeModelDef)
    }

    fun compose(articles: List<Article>, podcast: Podcast, composeModelDef: ModelDefinition): CompositionResult {
        log.info("[LLM] Composing dialogue from {} articles", articles.size)
        val chatClient = chatClientFactory.createForModel(podcast.userId, composeModelDef)
        val prompt = buildPrompt(articles, podcast)

        val (result, elapsed) = measureTimedValue {
            val chatResponse = chatClient.prompt()
                .user(prompt)
                .options(OpenAiChatOptions.builder().model(composeModelDef.model).build())
                .call()
                .chatResponse()

            val script = chatResponse?.result?.output?.text
                ?: throw IllegalStateException("Empty response from LLM for dialogue composition")

            val usage = TokenUsage.fromChatResponse(chatResponse)
            CompositionResult(script, usage)
        }

        log.info("[LLM] Dialogue composed — {} words in {}", result.script.split("\\s+".toRegex()).size, elapsed)
        return result
    }

    internal fun buildPrompt(articles: List<Article>, podcast: Podcast): String {
        val targetWords = podcast.targetWords ?: appProperties.briefing.targetWords
        val speakerRoles = podcast.ttsVoices?.keys?.toList() ?: listOf("host", "cohost")
        val tagExamples = speakerRoles.joinToString("\n            ") { role -> "<$role>Example text</$role>" }

        val summaryBlock = articles.mapIndexed { index, article ->
            val source = extractDomain(article.url)
            val authorSuffix = article.author?.let { ", by $it" } ?: ""
            val content = article.summary ?: article.body
            "${index + 1}. [$source$authorSuffix] ${article.title}\n$content"
        }.joinToString("\n\n")

        val customInstructionsBlock = podcast.customInstructions?.let {
            "\n\nAdditional instructions: $it"
        } ?: ""

        val locale = SupportedLanguage.fromCode(podcast.language)?.toLocale() ?: Locale.ENGLISH
        val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", locale))

        val languageInstruction = if (podcast.language != "en") {
            val langName = SupportedLanguage.fromCode(podcast.language)?.displayName ?: "English"
            "\n            - Write the entire dialogue in $langName"
        } else ""

        return """
            You are writing a dialogue script for a podcast with multiple speakers. Create a natural, engaging conversation between the speakers about the topics below.

            Podcast: ${podcast.name}
            Topic: ${podcast.topic}
            Date: $currentDate
            Speakers: ${speakerRoles.joinToString(", ")}

            Requirements:
            - Write as a natural conversation between ${speakerRoles.size} speakers
            - The first speaker (${speakerRoles.first()}) drives the conversation, introduces topics, and transitions between stories
            - Other speakers provide reactions, analysis, follow-up questions, and different perspectives
            - Use XML-style tags for each speaker turn. The ONLY valid tags are: ${speakerRoles.joinToString(", ") { "<$it>" }}
            - Example format:
            $tagExamples
            - ALL text MUST be inside speaker tags — no text outside of tags
            - You MAY include emotion cues in square brackets inside tags, e.g. <${speakerRoles.first()}>[cheerfully] Welcome back!</${speakerRoles.first()}>
            - Target approximately $targetWords words
            - In the introduction, mention the podcast name, its topic, and today's date
            - Naturally attribute information to its source and credit original authors when known
            - End with a sign-off
            - Do NOT include any stage directions, sound effects, or non-spoken text outside of tags
            - Do NOT include any meta-commentary, notes, or disclaimers about the script itself$languageInstruction$customInstructionsBlock

            Article summaries:
            $summaryBlock
        """.trimIndent()
    }

    private fun extractDomain(url: String): String =
        try {
            URI(url).host?.removePrefix("www.") ?: url
        } catch (_: Exception) {
            url
        }
}

package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.ModelDefinition
import com.aisummarypodcast.podcast.SupportedLanguage
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PodcastStyle
import org.slf4j.LoggerFactory
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.measureTimedValue

data class CompositionResult(
    val script: String,
    val usage: TokenUsage
)

@Component
class BriefingComposer(
    private val appProperties: AppProperties,
    private val modelResolver: ModelResolver,
    private val chatClientFactory: ChatClientFactory
) {

    private val log = LoggerFactory.getLogger(javaClass)

    private val stylePrompts = mapOf(
        PodcastStyle.NEWS_BRIEFING to "You are a professional news anchor creating an audio briefing. Use a structured, authoritative tone with smooth transitions between topics.",
        PodcastStyle.CASUAL to "You are a friendly podcast host having a casual chat. Use a conversational, relaxed tone as if talking to a friend.",
        PodcastStyle.DEEP_DIVE to "You are an analytical podcast host doing a deep-dive exploration. Provide in-depth analysis and thoughtful commentary on each topic.",
        PodcastStyle.EXECUTIVE_SUMMARY to "You are creating a concise executive summary. Be fact-focused with minimal commentary. Get straight to the point."
    )

    fun compose(articles: List<Article>, podcast: Podcast, previousEpisodeRecap: String? = null): CompositionResult {
        val composeModelDef = modelResolver.resolve(podcast, PipelineStage.COMPOSE)
        return compose(articles, podcast, composeModelDef, previousEpisodeRecap)
    }

    fun compose(articles: List<Article>, podcast: Podcast, composeModelDef: ModelDefinition, previousEpisodeRecap: String? = null): CompositionResult {
        log.info("[LLM] Composing briefing from {} articles for podcast '{}' ({}) (style: {})", articles.size, podcast.name, podcast.id, podcast.style)
        val chatClient = chatClientFactory.createForModel(podcast.userId, composeModelDef)
        val prompt = buildPrompt(articles, podcast, previousEpisodeRecap)

        val (result, elapsed) = measureTimedValue {
            val chatResponse = chatClient.prompt()
                .user(prompt)
                .options(OpenAiChatOptions.builder().model(composeModelDef.model).build())
                .call()
                .chatResponse()

            val rawScript = chatResponse?.result?.output?.text
                ?: throw IllegalStateException("Empty response from LLM for briefing composition")

            val script = stripSectionHeaders(rawScript)
            val usage = TokenUsage.fromChatResponse(chatResponse)
            CompositionResult(script, usage)
        }

        log.info("[LLM] Briefing composed for podcast '{}' ({}) — {} words in {}", podcast.name, podcast.id, result.script.split("\\s+".toRegex()).size, elapsed)
        return result
    }

    internal fun buildPrompt(articles: List<Article>, podcast: Podcast, previousEpisodeRecap: String? = null): String {
        val targetWords = podcast.targetWords ?: appProperties.briefing.targetWords
        val stylePrompt = stylePrompts[podcast.style] ?: stylePrompts[PodcastStyle.NEWS_BRIEFING]!!

        val useFullBody = shouldUseFullBody(articles.size, podcast, appProperties.briefing.fullBodyThreshold)

        val summaryBlock = articles.mapIndexed { index, article ->
            val source = extractDomain(article.url)
            val authorSuffix = article.author?.let { ", by $it" } ?: ""
            val content = resolveArticleContent(article, useFullBody)
            "${index + 1}. [$source$authorSuffix] ${article.title}\n$content"
        }.joinToString("\n\n")

        val customInstructionsBlock = podcast.customInstructions?.let {
            "\n\nAdditional instructions: $it"
        } ?: ""

        val locale = SupportedLanguage.fromCode(podcast.language)?.toLocale() ?: Locale.ENGLISH
        val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", locale))

        val languageInstruction = if (podcast.language != "en") {
            val langName = SupportedLanguage.fromCode(podcast.language)?.displayName ?: "English"
            "\n            - Write the entire script in $langName"
        } else ""

        val recapBlock = previousEpisodeRecap?.let {
            """

            Previous episode context:
            $it

            - When today's topics relate to the previous episode, weave in specific references (e.g., "as we discussed last time...", "following up on what we covered previously...")
            - When today's topics are unrelated, include a brief one-liner referencing the previous episode in the introduction (e.g., "last episode we covered X and Y, today we're looking at...")"""
        } ?: ""

        return """
            $stylePrompt

            Compose the following article summaries into a coherent, engaging monologue suitable for a podcast episode.

            Podcast: ${podcast.name}
            Topic: ${podcast.topic}
            Date: $currentDate

            Requirements:
            - Use natural spoken language, not written style
            - Include smooth transitions between topics
            - Target approximately $targetWords words
            - In the introduction, mention the podcast name, its topic, and today's date
            - Naturally attribute information to its source and credit original authors when known (e.g., "as John Smith reports for TechCrunch") — do not over-cite
            - End with a sign-off
            - Do NOT include any stage directions, sound effects, section headers (like [Opening], [Closing], [Transition]), or non-spoken text
            - Do NOT include any meta-commentary, notes, or disclaimers about the script itself
            - ONLY discuss topics that are present in the article summaries below. Do NOT introduce facts, stories, or claims from outside the provided articles. If only a few articles are provided, produce a shorter script rather than padding with external knowledge$languageInstruction$customInstructionsBlock

            Article summaries:
            $summaryBlock$recapBlock
        """.trimIndent()
    }

    internal fun stripSectionHeaders(script: String): String =
        script
            .replace(Regex("(?m)^\\[.+?]\\s*\\n"), "")
            .replace(Regex("\\s*\\((?:Dit script|This script|Note:|Disclaimer:)[^)]*\\)\\s*$"), "")
            .trim()

}

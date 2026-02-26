package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.config.ModelDefinition
import com.aisummarypodcast.podcast.SupportedLanguage
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.Podcast
import org.slf4j.LoggerFactory
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.measureTimedValue

@Component
class InterviewComposer(
    private val appProperties: AppProperties,
    private val modelResolver: ModelResolver,
    private val chatClientFactory: ChatClientFactory
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun compose(articles: List<Article>, podcast: Podcast, previousEpisodeRecap: String? = null, ttsScriptGuidelines: String = ""): CompositionResult {
        val composeModelDef = modelResolver.resolve(podcast, PipelineStage.COMPOSE)
        return compose(articles, podcast, composeModelDef, previousEpisodeRecap, ttsScriptGuidelines)
    }

    fun compose(articles: List<Article>, podcast: Podcast, composeModelDef: ModelDefinition, previousEpisodeRecap: String? = null, ttsScriptGuidelines: String = ""): CompositionResult {
        log.info("[LLM] Composing interview from {} articles for podcast '{}' ({})", articles.size, podcast.name, podcast.id)
        val chatClient = chatClientFactory.createForModel(podcast.userId, composeModelDef)
        val prompt = buildPrompt(articles, podcast, previousEpisodeRecap, ttsScriptGuidelines)

        val (result, elapsed) = measureTimedValue {
            val chatResponse = chatClient.prompt()
                .user(prompt)
                .options(OpenAiChatOptions.builder().model(composeModelDef.model).build())
                .call()
                .chatResponse()

            val script = chatResponse?.result?.output?.text
                ?: throw IllegalStateException("Empty response from LLM for interview composition")

            val usage = TokenUsage.fromChatResponse(chatResponse)
            CompositionResult(script, usage)
        }

        log.info("[LLM] Interview composed for podcast '{}' ({}) — {} words in {}", podcast.name, podcast.id, result.script.split("\\s+".toRegex()).size, elapsed)
        return result
    }

    internal fun buildPrompt(articles: List<Article>, podcast: Podcast, previousEpisodeRecap: String? = null, ttsScriptGuidelines: String = ""): String {
        val targetWords = podcast.targetWords ?: appProperties.briefing.targetWords

        val interviewerName = podcast.speakerNames?.get("interviewer")
        val expertName = podcast.speakerNames?.get("expert")

        val nameInstruction = if (interviewerName != null && expertName != null) {
            "\n            - The interviewer's name is \"$interviewerName\" and the expert's name is \"$expertName\". Use names naturally in conversation — place them mid-sentence or at the end of questions, never as bare turn openers."
        } else {
            "\n            - Speakers should address each other without using names."
        }

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
            "\n            - Write the entire interview in $langName"
        } else ""

        val sponsorBlock = podcast.sponsor?.let { s ->
            val name = s["name"] ?: return@let ""
            val message = s["message"] ?: return@let ""
            """
            - Immediately after the introduction, the interviewer should include the sponsor message: "This podcast is brought to you by $name — $message."
            - End with a sign-off that includes a mention of the sponsor: $name"""
        } ?: ""

        val ttsGuidelinesBlock = if (ttsScriptGuidelines.isNotEmpty()) {
            "\n\n            TTS script formatting:\n            $ttsScriptGuidelines"
        } else ""

        val recapBlock = previousEpisodeRecap?.let {
            """

            Previous episode context:
            $it

            - When today's topics relate to the previous episode, the interviewer should naturally reference it (e.g., "We talked about this last time — any updates?", "Following up on what we discussed...")
            - When today's topics are unrelated, the interviewer should briefly mention the previous episode in the opening before moving to today's topics"""
        } ?: ""

        return """
            You are writing an interview-style podcast script between an interviewer and an expert. The interviewer acts as an audience surrogate — asking questions, bridging topics, and providing brief reactions. The expert delivers the news content, context, and analysis.

            Podcast: ${podcast.name}
            Topic: ${podcast.topic}
            Date: $currentDate

            Requirements:
            - The interviewer (~20% of words) asks questions, bridges between topics, and reacts briefly
            - The expert (~80% of words) delivers substantive news content, provides context, and offers analysis
            - Use XML-style tags for each speaker turn. The ONLY valid tags are: <interviewer>, <expert>
            - Example format:
            <interviewer>Example question or reaction</interviewer>
            <expert>Example detailed answer with analysis</expert>
            - ALL text MUST be inside speaker tags — no text outside of tags
            - Target approximately $targetWords words
            - In the introduction, the interviewer should mention the podcast name, its topic, and today's date$sponsorBlock
            - Naturally attribute information to its source and credit original authors when known
            - Do NOT include any stage directions, sound effects, or non-spoken text outside of tags
            - Do NOT include any meta-commentary, notes, or disclaimers about the script itself
            - ONLY discuss topics that are present in the article summaries below. Do NOT introduce facts, stories, or claims from outside the provided articles. If only a few articles are provided, produce a shorter script rather than padding with external knowledge
            - Speaker transitions must sound natural — do NOT start a turn with a bare name address (e.g., "Jarno, the market..."). Instead, use conversational bridges: reactions, follow-ups, or connectors before transitioning (e.g., "That's a great point. Now I'm curious about...", "Interesting — speaking of which...")
            - When using the other speaker's name, place it mid-sentence or at the end of a question (e.g., "What do you make of this, Jarno?") rather than as the first word of a turn
            - Vary transition patterns — not every handover needs a name, a reaction, or the same phrasing. Mix questions, reactions, bridges, and direct topic shifts$nameInstruction$languageInstruction$customInstructionsBlock

            Article summaries:
            $summaryBlock$ttsGuidelinesBlock$recapBlock
        """.trimIndent()
    }

}

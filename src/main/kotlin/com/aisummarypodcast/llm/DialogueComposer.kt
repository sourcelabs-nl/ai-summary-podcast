package com.aisummarypodcast.llm

import com.aisummarypodcast.config.AppProperties
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
class DialogueComposer(
    private val appProperties: AppProperties,
    private val modelResolver: ModelResolver,
    private val chatClientFactory: ChatClientFactory
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun compose(articles: List<Article>, podcast: Podcast, ttsScriptGuidelines: String = "", followUpAnnotations: Map<Long, String> = emptyMap()): CompositionResult {
        val composeModelDef = modelResolver.resolve(podcast, PipelineStage.COMPOSE)
        return compose(articles, podcast, composeModelDef, ttsScriptGuidelines, followUpAnnotations)
    }

    fun compose(articles: List<Article>, podcast: Podcast, composeModelDef: ResolvedModel, ttsScriptGuidelines: String = "", followUpAnnotations: Map<Long, String> = emptyMap()): CompositionResult {
        log.info("[LLM] Composing dialogue from {} articles for podcast '{}' ({})", articles.size, podcast.name, podcast.id)
        val chatClient = chatClientFactory.createForModel(podcast.userId, composeModelDef)
        val prompt = buildPrompt(articles, podcast, ttsScriptGuidelines, followUpAnnotations)

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

        log.info("[LLM] Dialogue composed for podcast '{}' ({}) — {} words in {}", podcast.name, podcast.id, result.script.split("\\s+".toRegex()).size, elapsed)
        return result
    }

    internal fun buildPrompt(articles: List<Article>, podcast: Podcast, ttsScriptGuidelines: String = "", followUpAnnotations: Map<Long, String> = emptyMap()): String {
        val targetWords = podcast.targetWords ?: appProperties.briefing.targetWords
        val speakerRoles = podcast.ttsVoices?.keys?.toList() ?: listOf("host", "cohost")
        val tagExamples = speakerRoles.joinToString("\n            ") { role -> "<$role>Example text</$role>" }

        val nameInstruction = if (podcast.speakerNames != null && podcast.speakerNames.isNotEmpty()) {
            val nameMapping = speakerRoles.mapNotNull { role ->
                podcast.speakerNames[role]?.let { name -> "$role is \"$name\"" }
            }.joinToString(", ")
            "\n            - Speaker names: $nameMapping. Use these names naturally in conversation while keeping role keys as XML tags."
        } else ""

        val useFullBody = shouldUseFullBody(articles.size, podcast, appProperties.briefing.fullBodyThreshold)

        val summaryBlock = buildArticleSummaryBlock(articles, useFullBody, followUpAnnotations)

        val customInstructionsBlock = podcast.customInstructions?.let {
            "\n\nAdditional instructions: $it"
        } ?: ""

        val locale = SupportedLanguage.fromCode(podcast.language)?.toLocale() ?: Locale.ENGLISH
        val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", locale))

        val languageInstruction = if (podcast.language != "en") {
            val langName = SupportedLanguage.fromCode(podcast.language)?.displayName ?: "English"
            "\n            - Write the entire dialogue in $langName"
        } else ""

        val sponsorBlock = podcast.sponsor?.let { s ->
            val name = s["name"] ?: return@let ""
            val message = s["message"] ?: return@let ""
            """
            - Immediately after the introduction, include the sponsor message: "This podcast is brought to you by $name — $message."
            - End with a sign-off that includes a mention of the sponsor: $name"""
        } ?: ""

        val ttsGuidelinesBlock = if (ttsScriptGuidelines.isNotEmpty()) {
            "\n\n            TTS script formatting:\n            $ttsScriptGuidelines"
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
            - Target approximately $targetWords words
            - In the introduction, mention the podcast name, its topic, and today's date$sponsorBlock
            - Naturally attribute information to its source and credit original authors when known
            - Do NOT include any stage directions, sound effects, or non-spoken text outside of speaker tags. Inside speaker tags, TTS-supported cues (described in the TTS formatting section below, if present) ARE allowed
            - Do NOT include any meta-commentary, notes, or disclaimers about the script itself
            - ONLY discuss topics that are present in the article summaries below. Do NOT introduce facts, stories, or claims from outside the provided articles. If only a few articles are provided, produce a shorter script rather than padding with external knowledge

            Engagement techniques:
            - HOOK OPENING: Do NOT start with a standard welcome. Instead, open with a provocative statement, surprising fact, or compelling question drawn from the most interesting article of the day. Then transition into the regular introduction
            - FRONT-LOAD THE BEST STORY: Lead with the most compelling or surprising article, not the order they appear in the summaries
            - CURIOSITY HOOKS: The ${speakerRoles.first()} should use rhetorical questions and teaser hooks before transitions (e.g., "But here's where it gets really interesting...", "So why should we care?", "You'd think that's the whole story, but..."). Create micro-curiosity loops that pull listeners forward
            - MID-ROLL CALLBACKS: Reference earlier topics later in the episode to create narrative cohesion (e.g., "Remember that thing we talked about earlier? Well, this connects directly...", "This ties back to what you said about..."). Cross-reference at least once per episode
            - SHORT SEGMENTS WITH SIGNPOSTING: Keep individual topic segments concise (roughly 60-90 seconds each). Use clear verbal signposts so listeners always know where they are (e.g., "Next up...", "Switching gears...", "Now for something completely different...")
            - NATURAL INTERRUPTIONS: Speakers should occasionally interrupt each other MID-TOPIC — not at the end of a complete explanation, but while the other speaker is still building their point. Keep each speaker turn to 3-5 sentences max, then have the other speaker jump in with a reaction, follow-up question, or interjection (e.g., "Wait — hold on, does that mean...", "Okay but that sounds like..."). The original speaker then continues in their NEXT turn. Aim for 3-4 interruptions per episode, spread across different topics
            - EMPHASIS ON IMPORTANT NEWS: When covering major announcements or surprising developments, convey their significance — use emphatic language, exclamation marks, and brief pauses to let important news land. Not everything is exciting; save the energy for what truly stands out

            Speaker transitions:
            - NEVER place two consecutive tags of the same speaker (e.g., <${speakerRoles.first()}>...</${speakerRoles.first()}><${speakerRoles.first()}>...</${speakerRoles.first()}> is FORBIDDEN). Every speaker turn MUST be followed by the OTHER speaker before the same speaker can speak again$nameInstruction$languageInstruction$customInstructionsBlock

            Article summaries:
            $summaryBlock$ttsGuidelinesBlock
        """.trimIndent()
    }

}

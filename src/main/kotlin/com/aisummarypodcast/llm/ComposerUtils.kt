package com.aisummarypodcast.llm

import com.aisummarypodcast.podcast.SupportedLanguage
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.Podcast
import java.net.URI
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun resolveArticleContent(article: Article, useFullBody: Boolean): String =
    if (useFullBody) article.body else (article.summary ?: article.body)

fun shouldUseFullBody(articleCount: Int, podcast: Podcast, defaultThreshold: Int): Boolean =
    articleCount < (podcast.fullBodyThreshold ?: defaultThreshold)

fun extractDomain(url: String): String =
    try {
        URI(url).host?.removePrefix("www.") ?: url
    } catch (_: Exception) {
        url
    }

fun buildArticleSummaryBlock(articles: List<Article>, useFullBody: Boolean, followUpAnnotations: Map<Long, String> = emptyMap()): String {
    val groupedByFollowUp = mutableMapOf<String?, MutableList<Pair<Int, Article>>>()
    articles.forEachIndexed { index, article ->
        val context = article.id?.let { followUpAnnotations[it] }
        groupedByFollowUp.getOrPut(context) { mutableListOf() }.add(index to article)
    }

    return groupedByFollowUp.entries.joinToString("\n\n") { (context, articlePairs) ->
        val header = context?.let { "[FOLLOW-UP: $it]\n" } ?: ""
        val block = articlePairs.joinToString("\n\n") { (index, article) ->
            val source = extractDomain(article.url)
            val authorSuffix = article.author?.let { ", by $it" } ?: ""
            val content = resolveArticleContent(article, useFullBody)
            "${index + 1}. [$source$authorSuffix] ${article.title}\n$content"
        }
        "$header$block"
    }
}

fun buildToneBlock(): String =
    if (LocalDate.now().dayOfWeek == DayOfWeek.FRIDAY) {
        "\n            - It's Friday! Go loose and have fun with it. Be playful, crack jokes, use humor freely, riff on the topics. Let the energy be high and the vibe relaxed, as if wrapping up the week with friends over drinks."
    } else {
        "\n            - Adopt a casual and lighthearted tone. Sprinkle in a few nuanced, witty jokes or observations related to the topics, but keep it subtle and don't overdo it."
    }

fun buildCurrentDate(language: String): String {
    val locale = SupportedLanguage.fromCode(language)?.toLocale() ?: Locale.ENGLISH
    return LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", locale))
}

fun buildCustomInstructionsBlock(customInstructions: String?): String =
    customInstructions?.let { "\n\nAdditional instructions: $it" } ?: ""

fun buildLanguageInstruction(language: String, scriptType: String): String =
    if (language != "en") {
        val langName = SupportedLanguage.fromCode(language)?.displayName ?: "English"
        "\n            - Write the entire $scriptType in $langName"
    } else ""

fun buildSponsorBlock(sponsor: Map<String, String>?, speakerPrefix: String = ""): String =
    sponsor?.let { s ->
        val name = s["name"] ?: return@let ""
        val message = s["message"] ?: return@let ""
        """
            - Immediately after the introduction, ${speakerPrefix}include the sponsor message: "This podcast is brought to you by $name — $message."
            - End with a sign-off that includes a mention of the sponsor: $name"""
    } ?: ""

fun buildTtsGuidelinesBlock(ttsScriptGuidelines: String): String =
    if (ttsScriptGuidelines.isNotEmpty()) {
        "\n\n            TTS script formatting:\n            $ttsScriptGuidelines"
    } else ""

fun extractDomainAndPath(url: String): String =
    try {
        val uri = URI(url)
        val domain = uri.host?.removePrefix("www.") ?: return url
        val path = uri.path?.trimEnd('/') ?: ""
        if (path.isEmpty()) domain else "$domain$path"
    } catch (_: Exception) {
        url
    }

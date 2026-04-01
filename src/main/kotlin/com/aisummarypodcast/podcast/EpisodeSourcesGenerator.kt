package com.aisummarypodcast.podcast

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.TopicGroupedArticle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Component
class EpisodeSourcesGenerator(private val appProperties: AppProperties) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun generate(episode: Episode, podcast: Podcast, articles: List<TopicGroupedArticle>): Path? {
        if (articles.isEmpty() && episode.recap == null) return null

        val date = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneOffset.UTC)
            .format(Instant.parse(episode.generatedAt))

        val html = buildString {
            appendLine("<!DOCTYPE html>")
            appendLine("<html lang=\"en\">")
            appendLine("<head>")
            appendLine("<meta charset=\"UTF-8\">")
            appendLine("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
            appendLine("<title>${escapeHtml(podcast.name)} - $date</title>")
            appendLine("<style>")
            appendLine("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 700px; margin: 2rem auto; padding: 0 1rem; color: #1a1a1a; line-height: 1.6; }")
            appendLine("h1 { font-size: 1.5rem; margin-bottom: 0.25rem; }")
            appendLine(".date { color: #666; font-style: italic; margin-bottom: 1.5rem; }")
            appendLine(".summary { margin-bottom: 1.5rem; }")
            appendLine("h2 { font-size: 1.1rem; border-bottom: 1px solid #ddd; padding-bottom: 0.3rem; }")
            appendLine("h3 { font-size: 1rem; margin-top: 1.25rem; margin-bottom: 0.5rem; color: #333; }")
            appendLine("ul { padding-left: 1.25rem; }")
            appendLine("li { margin-bottom: 0.5rem; }")
            appendLine("a { color: #c2410c; }")
            appendLine("</style>")
            appendLine("</head>")
            appendLine("<body>")
            appendLine("<h1>${escapeHtml(podcast.name)}</h1>")
            appendLine("<p class=\"date\">$date</p>")
            if (episode.recap != null) {
                appendLine("<div class=\"summary\">")
                appendLine("<h2>Summary</h2>")
                appendLine("<p>${escapeHtml(episode.recap!!)}</p>")
                appendLine("</div>")
            }
            if (articles.isNotEmpty()) {
                val hasTopics = articles.any { it.topicOrder != null }
                if (hasTopics) {
                    val discussed = articles.filter { it.topicOrder != null }
                    val additional = articles.filter { it.topicOrder == null }

                    appendLine("<h2>Topics Covered</h2>")
                    var currentTopic: String? = null
                    for (article in discussed) {
                        val topicLabel = article.topic ?: "Other"
                        if (topicLabel != currentTopic) {
                            if (currentTopic != null) appendLine("</ul>")
                            appendLine("<h3>${escapeHtml(topicLabel)}</h3>")
                            appendLine("<ul>")
                            currentTopic = topicLabel
                        }
                        appendLine("<li><a href=\"${escapeHtml(article.url)}\">${escapeHtml(truncateTitle(article.title))}</a></li>")
                    }
                    if (currentTopic != null) appendLine("</ul>")

                    if (additional.isNotEmpty()) {
                        appendLine("<h2>Additional Sources</h2>")
                        appendLine("<p class=\"date\">Background material used for context but not explicitly discussed in the episode.</p>")
                        val groupedByTopic = additional.groupBy { it.topic ?: "Uncategorized" }
                        for ((topic, topicArticles) in groupedByTopic) {
                            appendLine("<h3>${escapeHtml(topic)}</h3>")
                            appendLine("<ul>")
                            for (article in topicArticles) {
                                appendLine("<li><a href=\"${escapeHtml(article.url)}\">${escapeHtml(truncateTitle(article.title))}</a></li>")
                            }
                            appendLine("</ul>")
                        }
                    }
                } else {
                    appendLine("<h2>Sources</h2>")
                    appendLine("<ul>")
                    for (article in articles) {
                        appendLine("<li><a href=\"${escapeHtml(article.url)}\">${escapeHtml(truncateTitle(article.title))}</a></li>")
                    }
                    appendLine("</ul>")
                }
            }
            appendLine("</body>")
            appendLine("</html>")
        }

        val slug = deriveSlug(episode)
        val episodesDir = Path.of(appProperties.episodes.directory, podcast.id, "episodes")
        Files.createDirectories(episodesDir)
        val sourcesPath = episodesDir.resolve("$slug-sources.html")
        Files.writeString(sourcesPath, html)

        log.info("Generated sources.html for episode {} at {}", episode.id, sourcesPath)
        return sourcesPath
    }

    private fun truncateTitle(title: String): String =
        if (title.length > 120) title.take(120) + "..." else title

    private fun escapeHtml(text: String): String =
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    fun deriveSlug(episode: Episode): String {
        val audioPath = episode.audioFilePath
        if (audioPath != null) {
            val fileName = Path.of(audioPath).fileName.toString()
            return fileName.removeSuffix(".mp3")
        }
        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC)
            .format(Instant.parse(episode.generatedAt))
        return "briefing-$timestamp"
    }
}

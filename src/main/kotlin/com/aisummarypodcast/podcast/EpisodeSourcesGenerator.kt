package com.aisummarypodcast.podcast

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.store.Article
import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.Podcast
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

    fun generate(episode: Episode, podcast: Podcast, articles: List<Article>): Path? {
        if (articles.isEmpty() && episode.recap == null) return null

        val date = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneOffset.UTC)
            .format(Instant.parse(episode.generatedAt))

        val markdown = buildString {
            appendLine("# ${podcast.name}")
            appendLine()
            appendLine("**Episode date:** $date")
            appendLine()
            if (episode.recap != null) {
                appendLine("## Summary")
                appendLine()
                appendLine(episode.recap)
                appendLine()
            }
            if (articles.isNotEmpty()) {
                appendLine("## Sources")
                appendLine()
                for (article in articles) {
                    appendLine("- [${article.title}](${article.url})")
                }
            }
        }

        val slug = deriveSlug(episode)
        val episodesDir = Path.of(appProperties.episodes.directory, podcast.id, "episodes")
        Files.createDirectories(episodesDir)
        val sourcesPath = episodesDir.resolve("$slug-sources.txt")
        Files.writeString(sourcesPath, markdown)

        log.info("Generated sources.txt for episode {} at {}", episode.id, sourcesPath)
        return sourcesPath
    }

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

package com.aisummarypodcast.podcast

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.store.EpisodeRepository
import com.rometools.rome.feed.synd.SyndContentImpl
import com.rometools.rome.feed.synd.SyndEnclosureImpl
import com.rometools.rome.feed.synd.SyndEntryImpl
import com.rometools.rome.feed.synd.SyndFeedImpl
import com.rometools.rome.io.SyndFeedOutput
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneOffset
import java.util.Date

@Component
class FeedGenerator(
    private val episodeRepository: EpisodeRepository,
    private val appProperties: AppProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun generate(): String {
        val feed = SyndFeedImpl().apply {
            feedType = "rss_2.0"
            title = appProperties.feed.title
            link = appProperties.feed.baseUrl
            description = appProperties.feed.description
        }

        val episodes = episodeRepository.findAll().sortedByDescending { it.generatedAt }

        feed.entries = episodes.map { episode ->
            SyndEntryImpl().apply {
                val generatedInstant = Instant.parse(episode.generatedAt)
                title = "${appProperties.feed.title} - ${generatedInstant.atOffset(ZoneOffset.UTC).toLocalDate()}"
                link = "${appProperties.feed.baseUrl}/episodes/${Path.of(episode.audioFilePath).fileName}"
                publishedDate = Date.from(generatedInstant)
                description = SyndContentImpl().apply {
                    type = "text/plain"
                    value = episode.scriptText.take(500) + "..."
                }
                enclosures = listOf(SyndEnclosureImpl().apply {
                    url = "${appProperties.feed.baseUrl}/episodes/${Path.of(episode.audioFilePath).fileName}"
                    type = "audio/mpeg"
                    length = Files.size(Path.of(episode.audioFilePath))
                })
            }
        }

        val writer = StringWriter()
        SyndFeedOutput().output(feed, writer)
        val xml = writer.toString()

        log.info("Generated RSS feed with {} episodes", episodes.size)
        return xml
    }
}

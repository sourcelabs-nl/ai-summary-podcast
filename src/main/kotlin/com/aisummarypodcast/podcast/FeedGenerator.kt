package com.aisummarypodcast.podcast

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.User
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

    fun generate(podcast: Podcast, user: User): String {
        val feedTitle = "${appProperties.feed.title} - ${user.name} - ${podcast.name}"

        val feed = SyndFeedImpl().apply {
            feedType = "rss_2.0"
            title = feedTitle
            link = appProperties.feed.baseUrl
            description = appProperties.feed.description
            language = podcast.language
        }

        val episodes = episodeRepository.findByPodcastIdAndStatus(podcast.id, "GENERATED").sortedByDescending { it.generatedAt }

        feed.entries = episodes.mapNotNull { episode ->
            val audioPath = episode.audioFilePath ?: return@mapNotNull null
            SyndEntryImpl().apply {
                val generatedInstant = Instant.parse(episode.generatedAt)
                title = "$feedTitle - ${generatedInstant.atOffset(ZoneOffset.UTC).toLocalDate()}"
                link = "${appProperties.feed.baseUrl}/episodes/${podcast.id}/${Path.of(audioPath).fileName}"
                publishedDate = Date.from(generatedInstant)
                description = SyndContentImpl().apply {
                    type = "text/plain"
                    value = episode.scriptText.take(500) + "..."
                }
                enclosures = listOf(SyndEnclosureImpl().apply {
                    url = "${appProperties.feed.baseUrl}/episodes/${podcast.id}/${Path.of(audioPath).fileName}"
                    type = "audio/mpeg"
                    length = try {
                        Files.size(Path.of(audioPath))
                    } catch (_: Exception) {
                        0L
                    }
                })
            }
        }

        val writer = StringWriter()
        SyndFeedOutput().output(feed, writer)
        val xml = writer.toString()

        log.info("Generated RSS feed for podcast {} with {} episodes", podcast.id, episodes.size)
        return xml
    }
}

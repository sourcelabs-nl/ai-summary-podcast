package com.aisummarypodcast.podcast

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path

@Component
class StaticFeedExporter(
    private val feedGenerator: FeedGenerator,
    private val userRepository: UserRepository,
    private val appProperties: AppProperties
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun export(podcast: Podcast) {
        try {
            val user = userRepository.findById(podcast.userId).orElse(null)
            if (user == null) {
                log.warn("Cannot export static feed for podcast {}: user {} not found", podcast.id, podcast.userId)
                return
            }

            val baseUrl = appProperties.feed.staticBaseUrl ?: appProperties.feed.baseUrl
            val xml = feedGenerator.generate(podcast, user, baseUrl)

            val podcastDir = Path.of(appProperties.episodes.directory, podcast.id)
            Files.createDirectories(podcastDir)
            Files.writeString(podcastDir.resolve("feed.xml"), xml)

            log.info("Exported static feed.xml for podcast {}", podcast.id)
        } catch (e: Exception) {
            log.warn("Failed to export static feed.xml for podcast {}: {}", podcast.id, e.message)
        }
    }
}

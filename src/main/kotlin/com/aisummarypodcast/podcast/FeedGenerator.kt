package com.aisummarypodcast.podcast

import com.aisummarypodcast.config.AppProperties
import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.EpisodePublicationRepository
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.store.EpisodeStatus
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.User
import com.rometools.modules.itunes.FeedInformationImpl
import com.rometools.rome.feed.synd.SyndContentImpl
import com.rometools.rome.feed.synd.SyndEnclosureImpl
import com.rometools.rome.feed.synd.SyndEntryImpl
import com.rometools.rome.feed.synd.SyndFeedImpl
import com.rometools.rome.feed.synd.SyndImageImpl
import com.rometools.rome.feed.rss.Channel
import com.rometools.rome.io.SyndFeedOutput
import com.rometools.rome.io.WireFeedOutput
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
    private val appProperties: AppProperties,
    private val podcastImageService: PodcastImageService,
    private val episodeSourcesGenerator: EpisodeSourcesGenerator,
    private val publicationRepository: EpisodePublicationRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun generate(podcast: Podcast, user: User, baseUrl: String? = null, publicUrl: String? = null, publishedTarget: String? = null): String {
        val effectiveBaseUrl = baseUrl ?: appProperties.feed.baseUrl
        // When publicUrl is set (FTP), files are relative to it (episodes/, podcast-image.*)
        // When local, files are at /data/{podcastId}/episodes/{file}
        val contentBaseUrl = publicUrl ?: "$effectiveBaseUrl/data/${podcast.id}/"
        val feedTitle = podcast.name

        val feed = SyndFeedImpl().apply {
            feedType = "rss_2.0"
            title = feedTitle
            link = effectiveBaseUrl
            description = appProperties.feed.description
            language = podcast.language
            publishedDate = Date.from(Instant.now())
        }

        val imagePath = podcastImageService.get(podcast.id)
        if (imagePath != null) {
            feed.image = SyndImageImpl().apply {
                url = "${contentBaseUrl}${imagePath.fileName}"
                title = feedTitle
                link = effectiveBaseUrl
            }
        }

        val feedProps = appProperties.feed
        if (feedProps.ownerName != null || feedProps.author != null) {
            val itunes = FeedInformationImpl()
            feedProps.ownerName?.let { itunes.ownerName = it }
            feedProps.ownerEmail?.let { itunes.ownerEmailAddress = it }
            feedProps.author?.let { itunes.author = it }
            val modules = feed.modules.toMutableList()
            modules.add(itunes)
            feed.modules = modules
        }

        val allEpisodes = episodeRepository.findByPodcastIdAndStatus(podcast.id, EpisodeStatus.GENERATED.name)
        val episodes = if (publishedTarget != null) {
            val publishedEpisodeIds = publicationRepository.findPublishedByPodcastIdAndTarget(podcast.id, publishedTarget)
                .map { it.episodeId }.toSet()
            allEpisodes.filter { it.id in publishedEpisodeIds }
        } else {
            allEpisodes
        }.sortedByDescending { it.generatedAt }

        feed.entries = episodes.mapNotNull { episode ->
            val audioPath = episode.audioFilePath ?: return@mapNotNull null
            val audioFileName = Path.of(audioPath).fileName
            SyndEntryImpl().apply {
                val generatedInstant = Instant.parse(episode.generatedAt)
                title = "$feedTitle - ${generatedInstant.atOffset(ZoneOffset.UTC).toLocalDate()}"
                link = "${contentBaseUrl}episodes/$audioFileName"
                publishedDate = Date.from(generatedInstant)
                description = SyndContentImpl().apply {
                    type = "text/plain"
                    value = buildDescription(episode, contentBaseUrl)
                }
                enclosures = listOf(SyndEnclosureImpl().apply {
                    url = "${contentBaseUrl}episodes/$audioFileName"
                    this.type = "audio/mpeg"
                    length = try {
                        Files.size(Path.of(audioPath))
                    } catch (_: Exception) {
                        0L
                    }
                })
            }
        }

        val wireFeed = feed.createWireFeed() as Channel
        wireFeed.ttl = 60

        val writer = StringWriter()
        WireFeedOutput().output(wireFeed, writer)
        val xml = writer.toString()

        log.info("Generated RSS feed for podcast {} with {} episodes", podcast.id, episodes.size)
        return xml
    }

    private fun buildDescription(episode: Episode, contentBaseUrl: String): String {
        val recap = episode.showNotes ?: episode.recap ?: (episode.scriptText.take(500) + "...")
        val slug = episodeSourcesGenerator.deriveSlug(episode)
        return "$recap\n\nSources: ${contentBaseUrl}episodes/$slug-sources.txt"
    }
}

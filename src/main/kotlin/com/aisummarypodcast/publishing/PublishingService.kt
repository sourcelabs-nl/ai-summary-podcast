package com.aisummarypodcast.publishing

import com.aisummarypodcast.podcast.StaticFeedExporter
import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.EpisodePublication
import com.aisummarypodcast.store.EpisodePublicationRepository
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.store.EpisodeStatus
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PublicationStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class PublishingService(
    private val publisherRegistry: PublisherRegistry,
    private val publicationRepository: EpisodePublicationRepository,
    private val episodeRepository: EpisodeRepository,
    private val soundCloudPublisher: SoundCloudPublisher,
    private val targetService: PodcastPublicationTargetService,
    private val staticFeedExporter: StaticFeedExporter
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun publish(episode: Episode, podcast: Podcast, userId: String, target: String): EpisodePublication {
        val publisher = publisherRegistry.getPublisher(target)
            ?: throw IllegalArgumentException("Unsupported publish target: $target")

        val publicationTarget = targetService.get(podcast.id, target)
        if (publicationTarget == null || !publicationTarget.enabled) {
            throw IllegalStateException("Publication target '$target' is not configured or enabled for this podcast")
        }

        if (episode.status != EpisodeStatus.GENERATED) {
            throw IllegalStateException("Episode must be in GENERATED status to publish (current: ${episode.status})")
        }

        if (episode.audioFilePath == null) {
            throw IllegalStateException("Episode has no audio file")
        }

        val existing = publicationRepository.findByEpisodeIdAndTarget(episode.id!!, target)

        if (existing?.status == PublicationStatus.PUBLISHED && existing.externalId != null) {
            return updateExisting(publisher, episode, podcast, userId, existing)
        }

        val now = Instant.now().toString()
        val publication = publicationRepository.save(
            EpisodePublication(
                id = existing?.id,
                episodeId = episode.id,
                target = target,
                status = PublicationStatus.PENDING,
                createdAt = existing?.createdAt ?: now
            )
        )

        return try {
            log.info("Publishing episode {} to {}", episode.id, target)
            val result = publisher.publish(episode, podcast, userId)

            val published = publicationRepository.save(
                publication.copy(
                    status = PublicationStatus.PUBLISHED,
                    externalId = result.externalId,
                    externalUrl = result.externalUrl,
                    publishedAt = Instant.now().toString()
                )
            )
            log.info("Episode {} published to {} (externalId={})", episode.id, target, result.externalId)
            staticFeedExporter.export(podcast)
            published
        } catch (e: Exception) {
            log.error("Failed to publish episode {} to {}: {}", episode.id, target, e.message, e)
            publicationRepository.save(
                publication.copy(
                    status = PublicationStatus.FAILED,
                    errorMessage = e.message
                )
            )
            throw e
        }
    }

    private fun updateExisting(
        publisher: EpisodePublisher,
        episode: Episode,
        podcast: Podcast,
        userId: String,
        existing: EpisodePublication
    ): EpisodePublication {
        return try {
            log.info("Updating episode {} on {} (externalId={})", episode.id, existing.target, existing.externalId)
            val result = publisher.update(episode, podcast, userId, existing.externalId!!)
            val updated = publicationRepository.save(
                existing.copy(
                    externalUrl = result.externalUrl,
                    publishedAt = Instant.now().toString(),
                    errorMessage = null
                )
            )
            log.info("Episode {} updated on {} (externalId={})", episode.id, existing.target, existing.externalId)
            staticFeedExporter.export(podcast)
            updated
        } catch (e: UnsupportedOperationException) {
            log.warn("Publisher {} does not support updates: {}", existing.target, e.message)
            throw e
        } catch (e: Exception) {
            log.error("Failed to update episode {} on {}: {}", episode.id, existing.target, e.message, e)
            publicationRepository.save(
                existing.copy(errorMessage = e.message)
            )
            throw e
        }
    }

    fun getPublications(episodeId: Long): List<EpisodePublication> =
        publicationRepository.findByEpisodeId(episodeId)

    fun rebuildSoundCloudPlaylist(podcast: Podcast, userId: String): List<Long> {
        val publications = publicationRepository.findPublishedByPodcastIdAndTarget(podcast.id, "soundcloud")
        require(publications.isNotEmpty()) { "No published SoundCloud tracks found for this podcast" }

        val trackIds = publications.mapNotNull { it.externalId?.toLongOrNull() }
        require(trackIds.isNotEmpty()) { "No valid SoundCloud track IDs found" }

        val episodeIds = publications.map { it.episodeId }.distinct()
        val episodes = episodeIds.mapNotNull { episodeRepository.findById(it).orElse(null) }
        soundCloudPublisher.updateTrackPermalinks(podcast, userId, episodes, publications)
        soundCloudPublisher.rebuildPlaylist(podcast, userId, trackIds)
        log.info("Updated permalinks and rebuilt SoundCloud playlist for podcast {} with {} tracks", podcast.id, trackIds.size)
        return trackIds
    }
}

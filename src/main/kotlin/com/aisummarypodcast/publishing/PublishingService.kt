package com.aisummarypodcast.publishing

import com.aisummarypodcast.store.Episode
import com.aisummarypodcast.store.EpisodePublication
import com.aisummarypodcast.store.EpisodePublicationRepository
import com.aisummarypodcast.store.EpisodeStatus
import com.aisummarypodcast.store.Podcast
import com.aisummarypodcast.store.PublicationStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class PublishingService(
    private val publisherRegistry: PublisherRegistry,
    private val publicationRepository: EpisodePublicationRepository
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun publish(episode: Episode, podcast: Podcast, userId: String, target: String): EpisodePublication {
        val publisher = publisherRegistry.getPublisher(target)
            ?: throw IllegalArgumentException("Unsupported publish target: $target")

        if (episode.status != EpisodeStatus.GENERATED) {
            throw IllegalStateException("Episode must be in GENERATED status to publish (current: ${episode.status})")
        }

        if (episode.audioFilePath == null) {
            throw IllegalStateException("Episode has no audio file")
        }

        val existing = publicationRepository.findByEpisodeIdAndTarget(episode.id!!, target)
        if (existing?.status == PublicationStatus.PUBLISHED) {
            throw IllegalStateException("Episode is already published to $target")
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

    fun getPublications(episodeId: Long): List<EpisodePublication> =
        publicationRepository.findByEpisodeId(episodeId)
}

package com.aisummarypodcast.podcast

import com.aisummarypodcast.store.ArticleRepository
import com.aisummarypodcast.store.EpisodeArticleRepository
import com.aisummarypodcast.store.EpisodeRepository
import com.aisummarypodcast.store.PodcastRepository
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/episodes")
class EpisodeMaintenanceController(
    private val episodeRepository: EpisodeRepository,
    private val podcastRepository: PodcastRepository,
    private val episodeArticleRepository: EpisodeArticleRepository,
    private val articleRepository: ArticleRepository,
    private val episodeSourcesGenerator: EpisodeSourcesGenerator
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping("/regenerate-show-notes")
    fun regenerateShowNotes(): ResponseEntity<Any> {
        val episodes = episodeRepository.findAll()
        var updatedShowNotes = 0
        var generatedSources = 0

        for (episode in episodes) {
            if (episode.recap != null && episode.showNotes != episode.recap) {
                episodeRepository.save(episode.copy(showNotes = episode.recap))
                updatedShowNotes++
            }

            val podcast = podcastRepository.findById(episode.podcastId).orElse(null) ?: continue
            try {
                val links = episodeArticleRepository.findByEpisodeId(episode.id!!)
                val articles = links.mapNotNull { link -> articleRepository.findById(link.articleId).orElse(null) }
                    .sortedByDescending { it.relevanceScore ?: 0 }
                if (episodeSourcesGenerator.generate(episode, podcast, articles) != null) {
                    generatedSources++
                }
            } catch (e: Exception) {
                log.warn("Failed to generate sources.md for episode {}: {}", episode.id, e.message)
            }
        }

        log.info("Regenerated show notes for {} episodes, generated sources.md for {} episodes", updatedShowNotes, generatedSources)
        return ResponseEntity.ok(mapOf(
            "updatedShowNotes" to updatedShowNotes,
            "generatedSources" to generatedSources,
            "totalEpisodes" to episodes.count()
        ))
    }
}

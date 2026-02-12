package com.aisummarypodcast.podcast

import com.aisummarypodcast.user.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class FeedController(
    private val feedGenerator: FeedGenerator,
    private val podcastService: PodcastService,
    private val userService: UserService
) {

    @GetMapping("/users/{userId}/podcasts/{podcastId}/feed.xml", produces = ["application/rss+xml"])
    fun feed(@PathVariable userId: String, @PathVariable podcastId: String): ResponseEntity<String> {
        val user = userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val podcast = podcastService.findById(podcastId) ?: return ResponseEntity.notFound().build()
        if (podcast.userId != userId) return ResponseEntity.notFound().build()
        return ResponseEntity.ok(feedGenerator.generate(podcast, user))
    }
}

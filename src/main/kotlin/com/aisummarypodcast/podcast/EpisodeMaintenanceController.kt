package com.aisummarypodcast.podcast

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/episodes")
class EpisodeMaintenanceController(
    private val episodeService: EpisodeService
) {

    @PostMapping("/regenerate-show-notes")
    fun regenerateShowNotes(): ResponseEntity<Any> {
        return ResponseEntity.ok(episodeService.regenerateAllShowNotes())
    }
}

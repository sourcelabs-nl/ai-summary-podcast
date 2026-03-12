package com.aisummarypodcast.publishing

import com.aisummarypodcast.user.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users/{userId}/publishing/test")
class PublishingTestController(
    private val userService: UserService,
    private val publishingTestService: PublishingTestService
) {

    @PostMapping("/ftp")
    fun testFtp(
        @PathVariable userId: String,
        @RequestBody credentials: FtpTestCredentials
    ): ResponseEntity<TestConnectionResult> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val result = publishingTestService.testFtp(credentials)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/soundcloud")
    fun testSoundCloud(
        @PathVariable userId: String
    ): ResponseEntity<TestConnectionResult> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        val result = publishingTestService.testSoundCloud(userId)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/{target}")
    fun testUnsupported(
        @PathVariable userId: String,
        @PathVariable target: String
    ): ResponseEntity<Any> {
        userService.findById(userId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.badRequest().body(mapOf("error" to "Unsupported target: $target"))
    }
}

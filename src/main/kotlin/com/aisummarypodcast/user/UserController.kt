package com.aisummarypodcast.user

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.net.URI

data class CreateUserRequest(val name: String)
data class UpdateUserRequest(val name: String)
data class UserResponse(val id: String, val name: String)

@RestController
@RequestMapping("/users")
class UserController(private val userService: UserService) {

    @PostMapping
    fun create(@RequestBody request: CreateUserRequest): ResponseEntity<UserResponse> {
        val user = userService.create(request.name)
        return ResponseEntity.created(URI.create("/users/${user.id}"))
            .body(UserResponse(user.id, user.name))
    }

    @GetMapping
    fun list(): List<UserResponse> =
        userService.findAll().map { UserResponse(it.id, it.name) }

    @GetMapping("/{userId}")
    fun get(@PathVariable userId: String): ResponseEntity<UserResponse> {
        val user = userService.findById(userId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(UserResponse(user.id, user.name))
    }

    @PutMapping("/{userId}")
    fun update(@PathVariable userId: String, @RequestBody request: UpdateUserRequest): ResponseEntity<UserResponse> {
        val user = userService.update(userId, request.name) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(UserResponse(user.id, user.name))
    }

    @DeleteMapping("/{userId}")
    fun delete(@PathVariable userId: String): ResponseEntity<Void> {
        if (!userService.delete(userId)) return ResponseEntity.notFound().build()
        return ResponseEntity.noContent().build()
    }
}

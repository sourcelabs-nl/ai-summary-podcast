package com.aisummarypodcast.user

import com.aisummarypodcast.publishing.OAuthConnectionService
import com.aisummarypodcast.store.User
import com.aisummarypodcast.store.UserProviderConfigRepository
import com.aisummarypodcast.store.UserRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val podcastService: com.aisummarypodcast.podcast.PodcastService,
    private val providerConfigRepository: UserProviderConfigRepository,
    private val oauthConnectionService: OAuthConnectionService
) {

    fun create(name: String): User {
        val user = User(id = UUID.randomUUID().toString(), name = name)
        return userRepository.save(user)
    }

    fun findAll(): List<User> = userRepository.findAll().toList()

    fun findById(userId: String): User? = userRepository.findById(userId).orElse(null)

    fun update(userId: String, name: String): User? {
        val user = findById(userId) ?: return null
        return userRepository.save(user.copy(name = name))
    }

    fun delete(userId: String): Boolean {
        val user = findById(userId) ?: return false
        podcastService.deleteAllByUserId(userId)
        providerConfigRepository.deleteByUserId(userId)
        oauthConnectionService.deleteByUserId(userId)
        userRepository.delete(user)
        return true
    }
}

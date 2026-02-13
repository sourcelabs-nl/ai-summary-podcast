package com.aisummarypodcast.publishing

import com.aisummarypodcast.config.ApiKeyEncryptor
import com.aisummarypodcast.store.OAuthConnection
import com.aisummarypodcast.store.OAuthConnectionRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class OAuthConnectionServiceTest {

    private val repository = mockk<OAuthConnectionRepository>(relaxed = true)
    private val encryptor = mockk<ApiKeyEncryptor>()
    private val service = OAuthConnectionService(repository, encryptor)

    @Test
    fun `storeConnection encrypts tokens and saves`() {
        every { repository.findByUserIdAndProvider("user1", "soundcloud") } returns null
        every { encryptor.encrypt("access-token") } returns "encrypted-access"
        every { encryptor.encrypt("refresh-token") } returns "encrypted-refresh"

        val slot = slot<OAuthConnection>()
        every { repository.save(capture(slot)) } answers { slot.captured }

        service.storeConnection(
            userId = "user1",
            provider = "soundcloud",
            accessToken = "access-token",
            refreshToken = "refresh-token",
            expiresAt = Instant.parse("2026-03-01T00:00:00Z"),
            scopes = "non-expiring"
        )

        val saved = slot.captured
        assertEquals("user1", saved.userId)
        assertEquals("soundcloud", saved.provider)
        assertEquals("encrypted-access", saved.encryptedAccessToken)
        assertEquals("encrypted-refresh", saved.encryptedRefreshToken)
        assertEquals("non-expiring", saved.scopes)
        assertNull(saved.id)
    }

    @Test
    fun `storeConnection updates existing connection`() {
        val existing = OAuthConnection(
            id = 42,
            userId = "user1",
            provider = "soundcloud",
            encryptedAccessToken = "old-encrypted",
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z"
        )
        every { repository.findByUserIdAndProvider("user1", "soundcloud") } returns existing
        every { encryptor.encrypt("new-access") } returns "new-encrypted-access"

        val slot = slot<OAuthConnection>()
        every { repository.save(capture(slot)) } answers { slot.captured }

        service.storeConnection("user1", "soundcloud", "new-access", null, null, null)

        val saved = slot.captured
        assertEquals(42L, saved.id)
        assertEquals("2026-01-01T00:00:00Z", saved.createdAt)
        assertEquals("new-encrypted-access", saved.encryptedAccessToken)
    }

    @Test
    fun `getConnection decrypts tokens`() {
        val connection = OAuthConnection(
            id = 1,
            userId = "user1",
            provider = "soundcloud",
            encryptedAccessToken = "encrypted-access",
            encryptedRefreshToken = "encrypted-refresh",
            expiresAt = "2026-03-01T00:00:00Z",
            scopes = "non-expiring",
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-02-01T00:00:00Z"
        )
        every { repository.findByUserIdAndProvider("user1", "soundcloud") } returns connection
        every { encryptor.decrypt("encrypted-access") } returns "access-token"
        every { encryptor.decrypt("encrypted-refresh") } returns "refresh-token"

        val result = service.getConnection("user1", "soundcloud")

        assertNotNull(result)
        assertEquals("access-token", result!!.accessToken)
        assertEquals("refresh-token", result.refreshToken)
        assertEquals(Instant.parse("2026-03-01T00:00:00Z"), result.expiresAt)
    }

    @Test
    fun `getConnection returns null when not found`() {
        every { repository.findByUserIdAndProvider("user1", "soundcloud") } returns null

        val result = service.getConnection("user1", "soundcloud")

        assertNull(result)
    }

    @Test
    fun `deleteConnection returns true when deleted`() {
        every { repository.deleteByUserIdAndProvider("user1", "soundcloud") } returns 1

        assertTrue(service.deleteConnection("user1", "soundcloud"))
    }

    @Test
    fun `deleteConnection returns false when not found`() {
        every { repository.deleteByUserIdAndProvider("user1", "soundcloud") } returns 0

        assertFalse(service.deleteConnection("user1", "soundcloud"))
    }

    @Test
    fun `getStatus returns connected when connection exists`() {
        val connection = OAuthConnection(
            id = 1,
            userId = "user1",
            provider = "soundcloud",
            encryptedAccessToken = "encrypted",
            scopes = "non-expiring",
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-02-01T00:00:00Z"
        )
        every { repository.findByUserIdAndProvider("user1", "soundcloud") } returns connection

        val status = service.getStatus("user1", "soundcloud")

        assertTrue(status.connected)
        assertEquals("non-expiring", status.scopes)
        assertEquals("2026-01-01T00:00:00Z", status.connectedAt)
    }

    @Test
    fun `getStatus returns not connected when no connection`() {
        every { repository.findByUserIdAndProvider("user1", "soundcloud") } returns null

        val status = service.getStatus("user1", "soundcloud")

        assertFalse(status.connected)
        assertNull(status.scopes)
    }

    @Test
    fun `deleteByUserId delegates to repository`() {
        service.deleteByUserId("user1")

        verify { repository.deleteByUserId("user1") }
    }
}

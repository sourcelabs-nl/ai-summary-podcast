package com.aisummarypodcast.publishing

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PublisherRegistryTest {

    @Test
    fun `getPublisher returns registered publisher`() {
        val publisher = mockk<EpisodePublisher> {
            every { targetName() } returns "soundcloud"
        }
        val registry = PublisherRegistry(listOf(publisher))

        assertEquals(publisher, registry.getPublisher("soundcloud"))
    }

    @Test
    fun `getPublisher returns null for unknown target`() {
        val publisher = mockk<EpisodePublisher> {
            every { targetName() } returns "soundcloud"
        }
        val registry = PublisherRegistry(listOf(publisher))

        assertNull(registry.getPublisher("youtube"))
    }

    @Test
    fun `empty registry returns null for any target`() {
        val registry = PublisherRegistry(emptyList())

        assertNull(registry.getPublisher("soundcloud"))
    }
}
